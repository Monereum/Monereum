# Welcome to Monereum Network

<img src="http://monereum.live/css/passed.svg"> <img src="http://monereum.live/css/code.svg"> <img src="http://monereum.live/css/qg.svg"> <a href="https://twitter.com/intent/user?screen_name=monereum"><img alt="Twitter Follow" src="https://img.shields.io/twitter/follow/monereum?style=social"></a>
# <p align="center"> <img src="https://github.com/Monereum/Monereum/blob/master/cmd/logo.png" width="200" height="200"/></p>


Our  team of crypto enthusiasts announced that the Monereum chain will be kept and running after the Monero hard fork scheduled for the 1th of November  2019 at block #1961130.

Monereum is an Ethereum-based distributed ledger protocol with transaction/contract privacy and special consensus mechanisms.

Monereum is based on [go-ethereum](https://github.com/ethereum/go-ethereum) and is updated in line with go-ethereum releases.

Web-Site: https://monereum.live

<img src="https://github.com/FortAwesome/Font-Awesome/blob/master/svgs/brands/telegram.svg" width="40" height="40"/> Telegram Channel: https://t.me/Monereum

<img src="https://github.com/FortAwesome/Font-Awesome/blob/master/svgs/brands/telegram.svg" width="40" height="40"/> Telegram Group: https://Monereumgroup

<img src="https://github.com/yammadev/brand-icons/blob/master/svg/twitter.svg" width="40" height="40"/> Twitter: https://twitter.com/Monereum

 <img src="https://github.com/yammadev/brand-icons/blob/master/svg/reddit.svg" width="40" height="40"/>Reddit: https://www.reddit.com/user/Monereum

<img src="https://github.com/yammadev/brand-icons/blob/master/svg/github.svg" width="40" height="40"/> Github: https://github.com/Monereum 

<img src="https://github.com/yammadev/brand-icons/blob/master/svg/telegram.svg" width="40" height="40"/> Airdrop bot: https://t.me/Monereumbot

Key enhancements over go-ethereum:

* __Privacy__ - Monereum supports private transactions and private contracts through public/private state separation, and utilises peer-to-peer encrypted message exchanges (see [Constellation](https://github.com/Monereum/Monereum/tree/master/constellation) and [Tessera](https://github.com/Monereum/Monereum/tree/master/tessera)) for directed transfer of private data to network participants
* __Alternative Consensus Mechanisms__ - with no need for POW/POS in a permissioned network, Monereum instead offers multiple consensus mechanisms that are more appropriate for consortium chains:
    * __Raft-based Consensus__ - a consensus model for faster blocktimes, transaction finality, and on-demand block creation
    * __Istanbul BFT__ - a PBFT-inspired consensus algorithm with transaction finality, by AMIS.
* __Peer Permissioning__ - node/peer permissioning using smart contracts, ensuring only known parties can join the network
* __Higher Performance__ - Monereum offers significantly higher performance than public geth

## Specifications

📌 Coin Supply - 298,693,496 MNR

📌 Circulating Supply at Hard Fork - ~ 17,3 Million MNR

📌 Consensus - 3 (Raft CFT, Istanbul pBFT, Clique POA Consensus)

📌 Algorithm - Ethash

📌 Block Time - ~ Every 20 seconds

📌 Block Reward -  Minimum of 6 MNR per block

📌 Block Size - Dynamic

📌 Privacy - Ring signature / stealth addresses

## See also

* [monereum-examples](https://github.com/monereum/monereum/monereum-examples): Monereum demonstration examples
* Monereum Transaction Managers
   * [Constellation](https://github.com/Monereum/Monereum/tree/master/constellation): Haskell implementation of peer-to-peer encrypted message exchange for transaction privacy
   * [Tessera](https://github.com/Monereum/Monereum/tree/master/tessera): Java implementation of peer-to-peer encrypted message exchange for transaction privacy
* Monereum supported consensuses
   * [Raft Consensus Documentation](https://docs.goquorum.com/en/latest/Consensus/raft/)
   * [Istanbul BFT Consensus Documentation](https://github.com/ethereum/EIPs/issues/650): [RPC API](https://docs.goquorum.com/en/latest/Consensus/istanbul-rpc-api/) and [technical article](https://medium.com/getamis/istanbul-bft-ibft-c2758b7fe6ff). __Please note__ that updated istanbul-tools is now hosted in [this](https://github.com/jpmorganchase/istanbul-tools/) repository
   * [Clique POA Consensus Documentation](https://github.com/ethereum/EIPs/issues/225) and a [guide to setup clique json](https://modalduality.org/posts/puppeth/) with [puppeth](https://blog.ethereum.org/2017/04/14/geth-1-6-puppeth-master/)
* Zero Knowledge on Monereum
   * [ZSL](https://github.com/jpmorganchase/quorum/wiki/ZSL) wiki page and [documentation](https://github.com/jpmorganchase/zsl-q/blob/master/README.md)
   * [Anonymous Zether](https://github.com/jpmorganchase/anonymous-zether) implementation
* [monereum-cloud](https://github.com/Monereum/Monereum/tree/master/monereum-cloud): Tools to help deploy Monereum network in a cloud provider of choice
* [Cakeshop](https://github.com/jpmorganchase/cakeshop): An integrated development environment and SDK for Monereum

## Third Party Tools / Libraries

The following Monereum-related libraries/applications have been created by Third Parties and as such are not specifically endorsed by J.P. Morgan.  A big thanks to the developers for improving the tooling around Monereum!

* [Monereum Blockchain Explorer](https://github.com/blk-io/epirus-free) - a Blockchain Explorer for Monereum which supports viewing private transactions
* [Monereum-Genesis](https://github.com/davebryson/quorum-genesis) - A simple CL utility for Monereum to help populate the genesis file with voters and makers
* [Monereum Maker](https://github.com/synechron-finlabs/quorum-maker/) - a utility to create Monereum nodes
* [MonereumNetworkManager](https://github.com/ConsenSys/QuorumNetworkManager) - makes creating & managing Monereum networks easy
* [ERC20 REST service](https://github.com/blk-io/erc20-rest-service) - a Monereum-supported RESTful service for creating and managing ERC-20 tokens
* [Nethereum Quorum](https://github.com/Nethereum/Nethereum/tree/master/src/Nethereum.Quorum) - a .NET Monereum adapter
* [web3j-monereum](https://github.com/web3j/quorum) - an extension to the web3j Java library providing support for the Monereum API
* [Apache Camel](http://github.com/apache/camel) - an Apache Camel component providing support for the Monereum API using web3j library. Here is the artcile describing how to use Apache Camel with Ethereum and Monereum https://medium.com/@bibryam/enterprise-integration-for-ethereum-fa67a1577d43

## Reporting Security Bugs
Security is part of our commitment to our users. At Monereum we have a close relationship with the security community, we understand the realm, and encourage security researchers to become part of our mission of building secure reliable software. This section explains how to submit security bugs, and what to expect in return.


#### Responsible Disclosure Process
Monereum project uses the following responsible disclosure process:

- Once the security report is received it is assigned a primary handler. This person coordinates the fix and release process.
- The issue is confirmed and a list of affected software is determined.
- Code is audited to find any potential similar problems.
- If it is determined, in consultation with the submitter, that a CVE-ID is required, the primary handler will trigger the process.
- Fixes are applied to the public repository and a new release is issued.
- On the date that the fixes are applied, announcements are sent to Monereum-announce.
- At this point you would be able to disclose publicly your finding.

**Note:** This process can take some time. Every effort will be made to handle the security bug in as timely a manner as possible, however it's important that we follow the process described above to ensure that disclosures are handled consistently.  

#### Receiving Security Updates
The best way to receive security announcements is to subscribe to the Monereum-announce mailing list/channel. Any messages pertaining to a security issue will be prefixed with **[security]**.

Comments on This Policy
If you have any suggestions to improve this policy, please send an email to support@monereum.live for discussion.

## License

The go-ethereum library (i.e. all code outside of the `cmd` directory) is licensed under the
[GNU Lesser General Public License v3.0](https://www.gnu.org/licenses/lgpl-3.0.en.html), also
included in our repository in the `COPYING.LESSER` file.

The go-ethereum binaries (i.e. all code inside of the `cmd` directory) is licensed under the
[GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.en.html), also included
in our repository in the `COPYING` file.
