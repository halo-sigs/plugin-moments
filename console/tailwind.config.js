/** @type {import('tailwindcss').Config} */
module.exports = {
  prefix: "moments-",
  content: ["./index.html", "./src/**/*.{vue,js,ts,jsx,tsx}"],
  theme: {},
  plugins: [require("@tailwindcss/aspect-ratio")],
};
