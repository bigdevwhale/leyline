# Changelog

<!--
Telegram: end each version's section with a hidden block for the StillCraft chat. Start it with a
line "<!-- tg", then 2-4 short lines in Russian for users (what is new, what is fixed), no markdown,
then close it like any HTML comment. The release notes carry it, and the chat bot posts those lines.
-->

## [1.1.2] - 2026-11-25

### Fixed
- **A long ride is no longer cut short.** A ride ended itself after 8 minutes without a good position, so a long jam or a stretch where the GPS went quiet lost the rest of the ride (25 km counted as 3). It now waits 20 minutes standing still (as long as a jam counts), 45 minutes when the phone hears no satellites at all, and rough positions that show the ride moving keep it going. The ride still ends where it last moved (#1).

<!-- tg
Исправлено: долгая поездка больше не обрывается сама через 8 минут. Теперь она ждёт 20 минут на месте и 45 минут, если пропал GPS, а заканчивается там, где вы остановились
-->

## [1.1.1] - 2026-11-25

### Added
- **Storm water and hoarfrost where the weather never brings them.** A strong wind (30 km/h and up) gives storm water, a cold night (+10° and below) gives hoarfrost, and the cauldron distils 4 of one ingredient into one of the sky's: storm water, hoarfrost or sun amber (#3).

### Fixed
- **The metro's stations load.** The busy public map servers rarely answered the old heavy question in time; the stations are now asked with a light one, given time, loaded ahead at start and kept for a month. The picker says what went wrong and can try again (#4).
- **«Reading the map along the way» no longer spins forever.** A slow answer kept the request hanging past its time limit, and a reading cut short when the app was closed was never tried again.
- **The text at the bottom is readable.** The floating dock no longer covers the last lines on phones with on-screen buttons (#2).

<!-- tg
Станции метро снова загружаются, а если карта не ответила – можно попробовать ещё раз
Грозовая вода и иней теперь есть и там, где не бывает дождя и снега: от сильного ветра, холодной ночью и перегонкой в котле (4 одинаковых → 1)
Исправлено: «Читаем карту вдоль пути» больше не крутится бесконечно, а текст внизу экрана не прячется под панелью
-->

## [1.1.0] - 2026-11-24

### Changed
- **The metro needs no GPS.** A ride by metro takes one position at the entrance and one at «Arrived», and the GPS sleeps in between instead of hunting for satellites under the ground. When a position can't be found, the station is picked from a list of the city's stations (from OpenStreetMap, nearest first, with search).

### Fixed
- **Long metro rides are no longer lost.** With no fixes under the ground, a metro ride ended itself after eight minutes at the entrance and was thrown away as too short. It now waits for the exit.

<!-- tg
Метро без GPS: приложение отмечает вход и выход, а между ними не тратит батарейку. Если сигнала нет – станцию можно выбрать из списка
Исправлено: поездки в метро дольше 8 минут больше не пропадают
-->

## [1.0.1] - 2026-11-22

### Fixed
- **A quarter keeps the kind it was visited with.** When the map servers didn't answer, a quarter got a stand-in kind, and the real answer later could turn a morning's «riverside» into evening «gardens» – with another spirit and other requests. A kind already seen on a visit now stays.

<!-- tg
Исправлено: квартал больше не меняет вид после того, как вы в нём побывали – его дух и просьбы остаются прежними
-->

## [1.0.0] - 2026-11-22

First release.

- Rides by car, bus, tram, underground, bike or as a passenger gather eighteen ingredients from the road itself (speed, jams, weather, night, twilight, the underground, new places) and from what lies along the route in OpenStreetMap.
- A cauldron for experiments: recipes are found by guessing their ingredients, with warm/cold hints; twenty-four recipes – elixirs, awakenings, wards, spirit gifts and three wonders.
- Five beasts, one per kind of ride, that grow from baby to ancient and earn traits from how you ride.
- The map of power: 250 m cells, paths that grow into great lines with rides, knots, underground dashes, sanctums with wards.
- Real districts become quarters of eight kinds, each with a spirit and four requests; long empty stretches get quarters of their own.
- The omen of the day, the blooming quarter of the week, road surprises, twelve cosy things for the tower.
- Nothing to tap while driving; a notification when the phone connects to your car; a quick-settings tile.
- Russian and English, light and dark themes.

<!-- tg
Новое приложение: ездите по городу как обычно, а дорога приносит ингредиенты для зелий
Рецепты нужно угадать, бросая ингредиенты в котёл. У каждого транспорта свой зверь, у каждого квартала – свой дух
За рулём ничего нажимать не нужно – всё собранное ждёт после стоянки
-->
