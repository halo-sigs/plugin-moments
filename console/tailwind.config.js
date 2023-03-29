/** @type {import('tailwindcss').Config} */
module.exports = {
  prefix: "moments-",
  content: ["./index.html", "./src/**/*.{vue,js,ts,jsx,tsx}"],
  theme: {
    extend: {
      width: {
        160: "40rem",
      },
      minHeight: {
        8: "2rem",
        36: "9rem",
      },
    },
  },
  plugins: [require("@tailwindcss/aspect-ratio")],
};
