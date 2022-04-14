<img src="https://elytrium.net/src/img/elytrium.webp" alt="Elytrium" align="right">

# LimboAPI (ex. ElytraProxy Virtual Server)

[![Join our Discord](https://img.shields.io/discord/775778822334709780.svg?logo=discord&label=Discord)](https://ely.su/discord)
[![Proxy Stats](https://img.shields.io/bstats/servers/12530?logo=minecraft&label=Servers)](https://bstats.org/plugin/velocity/LimboAPI/12530)
[![Proxy Stats](https://img.shields.io/bstats/players/12530?logo=minecraft&label=Players)](https://bstats.org/plugin/velocity/LimboAPI/12530)

Library for sending players to virtual servers (called limbo) \
[MC-Market](https://www.mc-market.org/resources/21097/) \
[SpigotMC.org](https://www.spigotmc.org/resources/limboapi-limboauth-limbofilter.95748/) \
[Описание и обсуждение на русском языке (spigotmc.ru)](https://spigotmc.ru/resources/limboapi-limboauth-limbofilter-virtualnye-servera-dlja-velocity.715/) \
[Описание и обсуждение на русском языке (rubukkit.org)](http://rubukkit.org/threads/limboapi-limboauth-limbofilter-virtualnye-servera-dlja-velocity.177904/)

Test server: [``ely.su``](https://hotmc.ru/minecraft-server-203216)

## See also

- [LimboAuth](https://github.com/Elytrium/LimboAuth) - Auth System built in virtual server (Limbo). Uses BCrypt, has TOTP 2FA feature. Supports literally any database due to OrmLite.
- [LimboFilter](https://github.com/Elytrium/LimboFilter) - Most powerful bot filtering solution for Minecraft proxies. Built with LimboAPI.

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

## How to include it

#### Setup your project via adding our maven repository to your pom.xml or build.gradle file.

- Maven:

```xml
    <repositories>
        <repository>
            <id>elytrium-repo</id>
            <url>https://maven.elytrium.net/repo/</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>net.elytrium</groupId>
            <artifactId>limboapi-api</artifactId>
            <version>1.0.3</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
```

- Gradle:

```groovy
    repositories {
        maven {
            setName("elytrium-repo")
            setUrl("https://maven.elytrium.net/repo/")
        }
    }

    dependencies {
        compileOnly("net.elytrium:limboapi-api:1.0.3")
    }
```

## Used Open Source projects

- [ProtocolSupport](https://github.com/ProtocolSupport/ProtocolSupport) - for modern->legacy block mappings
- [ViaVersion](https://github.com/ViaVersion/ViaVersion) - for modern string->integer block mappings

## Demo

- [LimboAuth](https://github.com/Elytrium/LimboAuth) - The auth plugin, that uses LimboAPI as a dependency at the basic level.
- [LimboFilter](https://github.com/Elytrium/LimboFilter) - The antibot solution, that uses LimboAPI as a dependency, using almost all available API methods, like Low-level Minecraft packet control.

## Donation

Your donations are really appreciated. Donations wallets/links/cards:

- MasterCard Debit Card (Tinkoff Bank): ``5536 9140 0599 1975``
- Qiwi Wallet: ``PFORG`` or [this link](https://my.qiwi.com/form/Petr-YSpyiLt9c6)
- YooMoney Wallet: ``4100 1721 8467 044`` or [this link](https://yoomoney.ru/quickpay/shop-widget?writer=seller&targets=Donation&targets-hint=&default-sum=&button-text=11&payment-type-choice=on&mobile-payment-type-choice=on&hint=&successURL=&quickpay=shop&account=410017218467044)
- PayPal: ``ogurec332@mail.ru``
