/** @type {import('tailwindcss').Config} */
module.exports = {
  prefix: "moments-",
  content: ["./index.html", "./src/**/*.{vue,js,ts,jsx,tsx}"],
  plugins: [require("@tailwindcss/aspect-ratio")],
};
