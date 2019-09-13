[![Build Status](https://travis-ci.org/jpmorganchase/tessera.svg?branch=master)](https://travis-ci.org/jpmorganchase/tessera)
[![codecov](https://codecov.io/gh/jpmorganchase/tessera/branch/master/graph/badge.svg?token=XMRVPC5FLQ)](https://codecov.io/gh/jpmorganchase/tessera)


# <img src="TesseraLogo.png" width="150" height="150"/>

> __Important: Release 0.9 Feature__ <br/>Tessera now supports remote enclaves for increased security. Please refer to the [wiki](https://github.com/jpmorganchase/tessera/wiki/What-is-an-Enclave%3F) for details. 

Tessera is a stateless Java system that is used to enable the encryption, decryption, and distribution of private transactions for [Quorum](https://github.com/jpmorganchase/quorum/).

Each Tessera node:

* Generates and maintains a number of private/public key pairs

* Self manages and discovers all nodes in the network (i.e. their public keys) by connecting to as few as one other node
    
* Provides Private and Public API interfaces for communication:
    * Private API - This is used for communication with Quorum
    * Public API - This is used for communication between Tessera peer nodes
    
* Provides two way SSL using TLS certificates and various trust models like Trust On First Use (TOFU), whitelist, 
    certificate authority, etc.
    
* Supports IP whitelist
  
* Connects to any SQL DB which supports the JDBC client

## Prerequisites
- [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html) (if using the [pre-built release jars](https://github.com/jpmorganchase/tessera/releases))
- [Java 8 - 11](https://www.oracle.com/technetwork/java/javase/downloads/index.html) (if building from source)
- [Maven](https://maven.apache.org) (if building from source)
- [libsodium](https://download.libsodium.org/doc/installation/) (if using kalium as the NaCl implementation)

## Building Tessera from source
To build and install Tessera:
1. Clone this repo
1. Build using Maven (see below)


### Selecting an NaCl Implementation 
Tessera can use either the [jnacl](https://github.com/neilalexander/jnacl) or [kalium](https://github.com/abstractj/kalium) NaCl cryptography implementations.  The implementation to be used is specified when building the project:

#### jnacl (default)

`mvn install`

#### kalium

Install libsodium as detailed on the [kalium project page](https://github.com/abstractj/kalium), then run
 
`mvn install -P kalium`


## Running Tessera
`java -jar tessera-dist/tessera-app/target/tessera-app-${version}-app.jar -configfile /path/to/config.json`

> See the [`tessera-dist` README](tessera-dist) for info on the different distributions available.

Once Tessera has been configured and built, you may want to copy the .jar to another location, create an alias and add it to your PATH:

`alias tessera="java -jar /path/to/tessera-app-${version}-app.jar"`

You will then be able to more concisely use the Tessera CLI commands, such as:

```
tessera -configfile /path/to/config.json
```

and

```
tessera help
```

By default, Tessera uses an H2 database.  To use an alternative database, add the necessary drivers to the classpath:

```
java -cp some-jdbc-driver.jar:/path/to/tessera-app.jar:. com.quorum.tessera.launcher.Main
```
For example, to use Oracle database: 
```
java -cp ojdbc7.jar:tessera-app.jar:. com.quorum.tessera.launcher.Main -configfile config.json
```

[DDLs](ddls/create-table) have been provided to help with defining these databases.

Since Tessera 0.7 a timestamp is recorded with each encrypted transaction stored in the Tessera DB.  To update an existing DB to work with Tessera 0.7+, execute one of the provided [alter scripts](ddls/add-timestamp).

## Configuration

### Config File

A configuration file detailing database, server and network peer information must be provided using the `-configfile`
command line property.

An in-depth look at configuring Tessera can be found on the [Tessera Wiki](https://github.com/jpmorganchase/tessera/wiki/Configuration-overview) and includes details on all aspects of configuration including:
* Cryptographic key config:
    * Using existing private/public key pairs with Tessera
    * How to use Tessera to generate new key pairs 
* TLS config
    * How to enable TLS
    * Choosing a trust mode
    
#### Obfuscate database password in config file

Certain entries in Tessera config file must be obfuscated in order to prevent any attempts from attackers to gain access to critical part of the application (i.e. database). For the time being, Tessera users have the ability to enable encryption for database password to avoid it being exposed as plain text in the configuration file.

In Tessera, [jasypt](http://www.jasypt.org) library was used together with its Jaxb integration to encrypt/decrypt config values.

To enable this feature, simply replace your plain-text database password with its encrypted value and wrap it inside an `ENC()` function.

```json
    "jdbc": {
        "username": "sa",
        "password": "ENC(ujMeokIQ9UFHSuBYetfRjQTpZASgaua3)",
        "url": "jdbc:h2:/qdata/c1/db1",
        "autoCreateTables": true
    }
```

Being a Password-Based Encryptor, Jasypt requires a secret key (password) and a configured algorithm to encrypt/decrypt this config entry. This password can either be loaded into Tessera from file system or user input. For file system input, the location of this secret file needs to be set in Environment Variable `TESSERA_CONFIG_SECRET`

If the database password is not being wrapped inside `ENC()` function, Tessera will simply treat it as a plain-text password however this approach is not recommended for production environment.

* Please note at the moment jasypt encryption is only enabled on `jdbc.password` field.

##### Encrypt database password

Download and unzip the [jasypt](http://www.jasypt.org) package. Redirect to bin directory and the follow commands can be used to encrypt a string

```bash
bash-3.2$ ./encrypt.sh input=dbpassword password=quorum

----ENVIRONMENT-----------------

Runtime: Oracle Corporation Java HotSpot(TM) 64-Bit Server VM 25.171-b11 



----ARGUMENTS-------------------

input: dbpassword
password: quorum



----OUTPUT----------------------

rJ70hNidkrpkTwHoVn2sGSp3h3uBWxjb

```

Pick up this output and wrap it inside `ENC()` function, we should have the following `ENC(rJ70hNidkrpkTwHoVn2sGSp3h3uBWxjb)` in the config json file.
 
### Migrating from Constellation to Tessera
Tessera is the service used to provide Quorum with the ability to support private transactions, replacing Constellation.  If you have previously been using Constellation, utilities are provided within Tessera to enable the migration of Constellation configuration and datastores to Tessera compatible formats.  Details on how to use these utilities can be found in the [Tessera Wiki](https://github.com/jpmorganchase/tessera/wiki/Migrating-from-Constellation).

## Further reading
* The [Tessera Wiki](https://github.com/jpmorganchase/tessera/wiki/) provides additional information on how Tessera works, migrating from Constellation to Tessera, configuration details, and more.
* [Quorum](https://github.com/jpmorganchase/quorum/) is an Ethereum-based distributed ledger protocol that uses Tessera to provide transaction privacy.
* Follow the [Quorum Examples](https://github.com/jpmorganchase/quorum-examples) to see Tessera in action in a demo Quorum network.

# Getting Help
Stuck at some step? Have no fear, the help is here <a href="https://clh7rniov2.execute-api.us-east-1.amazonaws.com/Express/" target="_blank" rel="noopener"><img title="Quorum Slack" src="https://clh7rniov2.execute-api.us-east-1.amazonaws.com/Express/badge.svg" alt="Quorum Slack" /></a>
