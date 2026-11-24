# Changelog

<!--
Telegram: end each version's section with a hidden block for the StillCraft chat. Start it with a
line "<!-- tg", then 2-4 short lines in Russian for users (what is new, what is fixed), no markdown,
then close it like any HTML comment. The release notes carry it, and the chat bot posts those lines.
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
