<img src="https://elytrium.net/src/img/elytrium.webp" alt="Elytrium" align="right">

# LimboAPI (ex. ElytraProxy Virtual Server)
[![Join our Discord](https://img.shields.io/discord/775778822334709780.svg?logo=discord&label=Discord)](https://ely.su/discord)
[![Proxy Stats](https://img.shields.io/bstats/servers/12530?logo=minecraft&label=Servers)](https://bstats.org/plugin/velocity/ElytraProxy/12271)
[![Proxy Stats](https://img.shields.io/bstats/players/12530?logo=minecraft&label=Players)](https://bstats.org/plugin/velocity/ElytraProxy/12271)

Library for sending players to virtual servers (called limbo)<br>
[Описание и обсуждение на русском языке (spigotmc.ru)](https://spigotmc.ru/resources/elytraproxy.715/) <br>
[Описание и обсуждение на русском языке (rubukkit.org)](http://rubukkit.org/threads/antibot-elytraproxy-proksi-server-fork-velocity-s-avtorizaciej-i-zaschitoj-ot-botov-1-7-1-17-1.177904/)

Test server: [``ely.su``](https://hotmc.ru/minecraft-server-203216)

## See also

- [LimboAuth](https://github.com/Elytrium/LimboAPI/tree/master/net.elytrium.auth) - Auth System built in virtual server (Limbo). Uses BCrypt, has TOTP 2FA feature. Supports literally any database due to OrmLite.
- [LimboFilter](https://github.com/Elytrium/Limbo/tree/master/net.elytrium.auth) - Most powerful bot filtering solution for Minecraft proxies. Built with LimboAPI.

### LimboFilter /vs/ popular antibot solutions:

Test server: i7-3770 (4c/8t 3.4GHz) Dedicated server, Ubuntu Server 20.04, OpenJDK 11, 16GB DDR3 1600MHz RAM, 4GB RAM is allocated to proxy. <br>
Attack: Motd + Join bot attack (100k joins per seconds, 1.17 Protocol)

Proxy server | Info | Boot time | % CPU on attack
--- | --- | --- | ---
Velocity | LimboFilter + LimboAuth Online/Offline Mode | 2 sec | 20%
Velocity | LimboFilter + Offline Mode | 2 sec | 20%
Leymooo's BungeeCord BotFilter | JPremium Online/Offline Mode | 8 sec | 95%
Leymooo's BungeeCord BotFilter | Offline Mode | 8 sec | 40%
yooniks' BungeeCord Aegis Escanor 1.3.1 | Offline Mode | 10 sec | 20%
yooniks' BungeeCord Aegis 9.2.1 | Offline Mode | 10 sec | 100% (what?)
Velocity | JPremium Online/Offline Mode | 2 sec | 95%
Velocity | Online Mode | 2 sec | 70%
Velocity | Offline Mode | 2 sec | 55%

## Features of LimboAPI

- Send to the Limbo server during login process
- Send to the Limbo server during play process
- Send maps, items to player's virtual inventory
- Display player's XP
- Send Title, Chat, ActionBar
- Load world from world files like .schematic
- and more...

## How to

- Include ``limboapi-api`` to your Maven/Gradle project as compile-only
- Subscribe to ``LoginLimboRegisterEvent`` to send players to the Limbo server during login process 
- Use ``LimboFactory`` to send players to the Limbo server during play process

### Demo

- [LimboAuth](https://github.com/Elytrium/LimboAPI/tree/master/net.elytrium.auth) - Simple usage, using special api
- [LimboFilter](https://github.com/Elytrium/Limbo/tree/master/net.elytrium.auth) - Advanced usage, using plugin's api

## Donation

Your donations are really appreciated. Donations wallets/links/cards:

- MasterCard Debit Card (Tinkoff Bank): ``5536 9140 0599 1975``
- Qiwi Wallet: ``PFORG`` or [this link](https://my.qiwi.com/form/Petr-YSpyiLt9c6)
- YooMoney Wallet: ``4100 1721 8467 044`` or [this link](https://yoomoney.ru/quickpay/shop-widget?writer=seller&targets=Donation&targets-hint=&default-sum=&button-text=11&payment-type-choice=on&mobile-payment-type-choice=on&hint=&successURL=&quickpay=shop&account=410017218467044)
- PayPal: ``ogurec332@mail.ru``