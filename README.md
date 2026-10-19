# Meteoritos (Fabric 1.20.1)

Lluvia de meteoritos estilo Dedsafio. **Solo servidor**: los jugadores no necesitan instalar nada.

- Meteoritos enormes: bola de fuego gigante con estela larga de fuego y humo oscuro (todo con particulas, nada de bolas de fuego de Minecraft).
- Explosion real al impactar (rompe bloques, deja magma y fuego) y crater ardiendo.
- **Hongo de humo estilo bomba atomica**: bola de fuego, tallo, sombrero que se infla y onda de choque.
- Temblor de camara segun la distancia (sin ceguera ni efectos de pocion).
- Pensado para hardcore: los meteoritos **nunca apuntan a los jugadores**; siempre caen lejos de todos.
- Barra roja "LLUVIA DE METEORITOS 01:46" y aviso "METEORITO 88 m ↑" con la distancia y la direccion.

## Comandos (nivel de operador)

| Comando | Que hace |
|---|---|
| `/meteoritos iniciar <minutos>` | Inicia la lluvia (1 a 180 min) |
| `/meteoritos detener` | Corta todo y borra los meteoritos |
| `/meteoritos intensidad <1-5>` | 1 Leve, 2 Moderada, 3 Fuerte, 4 Extrema, 5 Apocalipsis |
| `/meteoritos uno` | Lanza un meteorito delante de ti (para probar) |
| `/meteoritos destruir <true/false>` | Si las explosiones rompen bloques |
| `/meteoritos estado` | Muestra el estado actual |

## Compilar

Sube el proyecto a GitHub: la accion de `.github/workflows/build.yml` compila sola.
Entra a **Actions**, abre la ultima ejecucion y descarga el artefacto `meteoritos-jar`.

Localmente: `gradle build` (Java 17, Gradle 8.5+). El jar queda en `build/libs/`.

## Instalar

1. Servidor Fabric 1.20.1 (Aternos lo tiene) con **Fabric API**.
2. Pon `meteoritos-1.0.0.jar` en la carpeta `mods`.
3. Singleplayer: instala Fabric Loader 1.20.1 + Fabric API y pon el jar en `mods`.
