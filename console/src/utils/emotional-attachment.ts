import type { MomentEmotion } from "@/api/generated";

export const moods = [
  "😊 开心",
  "😌 平静",
  "🥰 幸福",
  "🤩 期待",
  "🥺 想念",
  "😔 低落",
  "😴 疲惫",
  "😶 放空",
];

export function normalizeEmotion(value?: MomentEmotion): MomentEmotion | undefined {
  const mood = value?.mood?.trim();
  const location = value?.location?.trim();
  const weather = value?.weather?.trim();
  if (!mood && !location && !weather) {
    return undefined;
  }
  return {
    ...(mood ? { mood } : {}),
    ...(location ? { location } : {}),
    ...(weather ? { weather, weatherSource: value?.weatherSource } : {}),
  };
}

// WMO weather codes: https://open-meteo.com/en/docs#weathervariables
const weatherDescriptions: Record<number, string> = {
  0: "晴",
  1: "晴间多云",
  2: "多云",
  3: "阴",
  45: "雾",
  48: "雾凇",
  51: "小毛毛雨",
  53: "毛毛雨",
  55: "密集毛毛雨",
  56: "轻冻毛毛雨",
  57: "冻毛毛雨",
  61: "小雨",
  63: "中雨",
  65: "大雨",
  66: "轻冻雨",
  67: "冻雨",
  71: "小雪",
  73: "中雪",
  75: "大雪",
  77: "米雪",
  80: "小阵雨",
  81: "阵雨",
  82: "强阵雨",
  85: "小阵雪",
  86: "强阵雪",
  95: "雷雨",
  96: "雷雨伴冰雹",
  97: "强雷雨",
  99: "强雷雨伴冰雹",
};

async function fetchJson(url: URL, signal: AbortSignal): Promise<unknown> {
  const response = await fetch(url, { signal, credentials: "omit", referrerPolicy: "no-referrer" });
  if (!response.ok) {
    throw new Error("服务暂时不可用");
  }
  return response.json();
}

export async function fetchLocation(
  coords: GeolocationCoordinates,
  signal: AbortSignal
): Promise<string> {
  const url = new URL("https://api.bigdatacloud.net/data/reverse-geocode-client");
  url.search = new URLSearchParams({
    latitude: String(coords.latitude),
    longitude: String(coords.longitude),
    localityLanguage: "zh",
  }).toString();
  const data = (await fetchJson(url, signal)) as Record<string, unknown>;
  const location = [data.city, data.locality, data.principalSubdivision, data.countryName].find(
    (value): value is string => typeof value === "string" && !!value.trim()
  );
  if (!location) {
    throw new Error("未获取到城市名称");
  }
  return location.trim().slice(0, 100);
}

export async function fetchWeather(
  coords: GeolocationCoordinates,
  signal: AbortSignal
): Promise<string> {
  const url = new URL("https://api.open-meteo.com/v1/forecast");
  url.search = new URLSearchParams({
    latitude: String(coords.latitude),
    longitude: String(coords.longitude),
    current: "temperature_2m,weather_code",
    temperature_unit: "celsius",
  }).toString();
  const data = (await fetchJson(url, signal)) as {
    current?: { temperature_2m?: number; weather_code?: number };
  };
  const temperature = data.current?.temperature_2m;
  const code = data.current?.weather_code;
  if (typeof temperature !== "number" || !Number.isFinite(temperature) || code === undefined) {
    throw new Error("未获取到天气信息");
  }
  const description = weatherDescriptions[code];
  if (!description) {
    throw new Error("暂不支持此天气类型");
  }
  return `${description} · ${Math.round(temperature)}°C`;
}

export function getCurrentPosition(): Promise<GeolocationPosition> {
  if (!window.isSecureContext) {
    return Promise.reject(new Error("自动定位需要 HTTPS，请手动填写位置和天气"));
  }
  if (!navigator.geolocation) {
    return Promise.reject(new Error("当前浏览器不支持定位，请手动填写位置和天气"));
  }
  return new Promise((resolve, reject) => {
    navigator.geolocation.getCurrentPosition(
      resolve,
      (error) => {
        const messages: Record<number, string> = {
          1: "未获得定位权限，请在浏览器中允许定位，或手动填写",
          2: "暂时无法确定位置，请重试或手动填写",
          3: "定位超时，请重试或手动填写",
        };
        reject(new Error(messages[error.code] || "定位失败，请重试或手动填写"));
      },
      { enableHighAccuracy: false, timeout: 12000, maximumAge: 60000 }
    );
  });
}
