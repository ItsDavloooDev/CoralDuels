# CoralDuels

Plugin Paper per duelli PvP realistici con kit, statistiche MySQL, leaderboard e reward configurabili.

## Requisiti
- Paper 1.21.1+
- Java 21
- MySQL 8.0+

## Installazione
1. Scarica l'ultimo release JAR
2. Metti nella cartella `plugins/`
3. Avvia il server
4. Configura `config.yml` con le credenziali MySQL
5. Riavvia o usa `/dueladmin reload config`

## Comandi Giocatore
| Comando | Descrizione | Permesso |
|---------|-------------|----------|
| `/duel challenge <player> [kit]` | Sfida un player | `coralduels.duel.challenge` |
| `/duel accept` | Accetta una sfida | `coralduels.duel.accept` |
| `/duel deny` | Rifiuta una sfida | `coralduels.duel.deny` |
| `/duel leave` | Abbandona duello attivo | `coralduels.duel.leave` |
| `/duel stats [player]` | Statistiche personali | `coralduels.duel.stats` |
| `/duel top [category]` | Leaderboard (elo/wins/losses/played/streak/kills/deaths) | `coralduels.duel.top` |
| `/kit list` | Lista kit disponibili | `coralduels.kit.list` |
| `/kit select <kit>` | Seleziona e applica kit | `coralduels.kit.select` |
| `/kit preview <kit>` | Anteprima contenuto kit | `coralduels.kit.preview` |

## Comandi Admin
| Comando | Descrizione | Permesso |
|---------|-------------|----------|
| `/dueladmin reload [config/kits/arenas/rewards/messages]` | Ricarica configurazioni | `coralduels.admin` |
| `/dueladmin arena list` | Lista arene | `coralduels.admin` |
| `/dueladmin kit list` | Lista kit | `coralduels.admin` |
| `/dueladmin forceend <player>` | Forza fine duello | `coralduels.admin` |
| `/dueladmin stats <player>` | Statistiche player | `coralduels.admin` |

## Kit Default
- **classic** - Spada diamante, scudo, armatura ferro
- **archer** - Arco potenziato, frecce infinite, armatura cuoio
- **tank** - Armatura netherite, rigenerazione, totem

## Configurazione
- `config.yml` - Database, timer, limiti, mondo
- `kits.yml` - Definizione kit con item, armature, effetti, permessi
- `rewards.yml` - Reward per win/loss/draw/kit-specific (COMMAND, ITEM, MONEY, EXPERIENCE, PERMISSION)
- `messages.yml` - Tutti i messaggi con placeholder
- `arenas.yml` - Arene con spawn point e bounds
- `leaderboard.yml` - Configurazione leaderboard

## Placeholder Messaggi
- `%player%` - Nome player
- `%target%` - Target del comando
- `%challenger%` - Sfidante
- `%kit%` - Nome kit
- `%opponent%` - Avversario
- `%seconds%` - Secondi countdown
- `%reward%` - Reward ricevuto
- `%pos%` - Posizione leaderboard
- `%wins%` - Vittorie
- `%losses%` - Sconfitte
- `%elo%` - ELO rating
- `%stat%` / `%value%` - Statistiche

## Permessi Kit
- `coralduels.kit.select.classic`
- `coralduels.kit.select.archer`
- `coralduels.kit.select.tank`
- `coralduels.kit.select.*` - Tutti i kit

## Build
```bash
./gradlew shadowJar
```
Output in `build/libs/CoralDuels-<version>.jar`

## Database
Tabelle create automaticamente:
- `coralduels_stats` - Statistiche player
- `coralduels_history` - Storico duelli

## Release Notes v0.1.0
- Sistema duelli completo con richieste, accettazione, timeout
- Kit configurabili con permessi
- Stato player salvato/ripristinato (inventory, armor, health, exp, gamemode, effects, location)
- Arene multiple con spawn configurabili
- Statistiche MySQL async (HikariCP)
- Leaderboard per ELO, vittorie, giocate, streak, kill, morti
- Reward system flessibile (comandi console, item, money, exp, permission)
- Admin commands per gestione completa
- Protezioni: anti-cheat base, command/world blocking durante duello
- Cleanup robusto su quit/kick/death/disable
- Messaggi completamente configurabili