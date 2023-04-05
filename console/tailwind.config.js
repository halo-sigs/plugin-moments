/** @type {import('tailwindcss').Config} */
module.exports = {
  prefix: "moments-",
  content: ["./index.html", "./src/**/*.{vue,js,ts,jsx,tsx}"],
  theme: {
    container: {
      padding: {
        DEFAULT: "0.5rem",
        sm: "1.5rem",
        lg: "4rem",
        xl: "14rem",
        "2xl": "22rem",
      },
    },
  },
  plugins: [require("@tailwindcss/aspect-ratio")],
};
