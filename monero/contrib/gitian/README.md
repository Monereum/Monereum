Gitian building
================

*Setup instructions for a Gitian build of Monero.*

Gitian is the deterministic build process that is used to build the Monero CLI
executables. It provides a way to be reasonably sure that the
executables are really built from the git source. It also makes sure that
the same, tested dependencies are used and statically built into the executable.

Multiple developers build the source code by following a specific descriptor
("recipe"), cryptographically sign the result, and upload the resulting signature.
These results are compared and only if they match, the build is accepted and provided
for download.

Gitian runs compilation steps in an isolated container. It is flexible and gives you full
control over the build environment, while still ensuring reproducibility and consistent output
formats.

More independent Gitian builders are needed, which is why this guide exists.
It is preferred you follow these steps yourself instead of using someone else's
VM image to avoid 'contaminating' the build.

Preparing the Gitian builder host
---------------------------------

The first step is to prepare the host environment that will be used to perform the Gitian builds.
This guide explains how to set up the environment, and how to start the builds.

* Gitian host OS should be Ubuntu 18.04 "Bionic Beaver".  If you are on a mac or windows for example, you can run it in a VM but will be slower.

* Gitian gives you the option of using any of 3 different virtualization tools: `kvm`, `docker` or `lxc`. This documentation will only show how to build with `lxc` and `docker` (documentation for `kvm` is welcome). Building with `lxc` is the default, but is more complicated, so we recommend docker your first time.


## Create the gitianuser account

You need to create a new user called `gitianuser` and be logged in as that user.  The user needs `sudo` access.


LXC
---

LXC builds should be run on Ubuntu 18.04 "Bionic Beaver".

Note that a version of `lxc-execute` higher or equal to 2.1.1 is required.
You can check the version with `lxc-execute --version`.

First we need to set up dependencies. Type/paste the following in the terminal:

```bash
sudo apt-get install git ruby apt-cacher-ng qemu-utils debootstrap lxc python-cheetah parted kpartx bridge-utils make ubuntu-archive-keyring curl firewalld
```

Then set up LXC and the rest with the following, which is a complex jumble of settings and workarounds:

```bash
sudo -s
# the version of lxc-start in Debian needs to run as root, so make sure
# that the build script can execute it without providing a password
echo "%sudo ALL=NOPASSWD: /usr/bin/lxc-start" > /etc/sudoers.d/gitian-lxc
echo "%sudo ALL=NOPASSWD: /usr/bin/lxc-execute" >> /etc/sudoers.d/gitian-lxc
# make /etc/rc.local script that sets up bridge between guest and host
echo '#!/bin/sh -e' > /etc/rc.local
echo 'brctl addbr br0' >> /etc/rc.local
echo 'ip addr add 10.0.3.1/24 broadcast 10.0.3.255 dev br0' >> /etc/rc.local
echo 'ip link set br0 up' >> /etc/rc.local
echo 'firewall-cmd --zone=trusted --add-interface=br0' >> /etc/rc.local
echo 'exit 0' >> /etc/rc.local
chmod +x /etc/rc.local
# make sure that USE_LXC is always set when logging in as gitianuser,
# and configure LXC IP addresses
echo 'export USE_LXC=1' >> /home/gitianuser/.profile
echo 'export GITIAN_HOST_IP=10.0.3.1' >> /home/gitianuser/.profile
echo 'export LXC_GUEST_IP=10.0.3.5' >> /home/gitianuser/.profile
reboot
```

This setup is required to enable networking in the container.

Docker
------

Prepare for building with docker:

```bash
sudo apt-get install git make curl docker.io
```

Consider adding `gitianuser` to the `docker` group after reading about [the security implications](https://docs.docker.com/v17.09/engine/installation/linux/linux-postinstall/):

```bash
sudo groupadd docker
sudo usermod -aG docker gitianuser
```

Optionally add yourself to the docker group. Note that this will give docker root access to your system.

```bash
sudo usermod -aG docker gitianuser
```

Manual Building
-------------------

The instructions below use the automated script [gitian-build.py](gitian-build.py) which only works in Ubuntu. 
=======
The script automatically installs some packages with apt. If you are not running it on a debian-like system, pass `--no-apt` along with the other
arguments to it. It calls all available .yml descriptors, which in turn pass the build configurations for different platforms to gitian.
The instructions below use the automated script [gitian-build.py](gitian-build.py) which is tested to work on Ubuntu.

It calls all available .yml descriptors, which in turn pass the build configurations for different platforms to gitian.
Help for the build steps taken can be accessed with `./gitian-build.py --help`.

Initial Gitian Setup
--------------------

The `gitian-build.py` script will checkout different release tags, so it's best to copy it to the top level directory:

```bash
cp monero/contrib/gitian/gitian-build.py .
```

### Setup the required environment

Setup for LXC:

```bash
GH_USER=fluffypony
VERSION=v0.14.1.0

./gitian-build.py --setup $GH_USER $VERSION
```

Where `GH_USER` is your Github user name and `VERSION` is the version tag you want to build. 

Setup for docker:

```bash
./gitian-build.py --setup --docker $GH_USER $VERSION
```

While gitian and this build script does provide a way for you to sign the build directly, it is recommended to sign in a separate step. This script is only there for convenience. Separate steps for building can still be taken.
In order to sign gitian builds on your host machine, which has your PGP key, 
fork the [gitian.sigs repository](https://github.com/monero-project/gitian.sigs) and clone it on your host machine, 
or pass the signed assert file back to your build machine.

```bash
git clone git@github.com:monero-project/gitian.sigs.git
git remote add $GH_USER git@github.com:$GH_USER/gitian.sigs.git
```

Build the binaries
------------------

**Note:** if you intend to build MacOS binaries, please follow [these instructions](https://github.com/bitcoin-core/docs/blob/master/gitian-building/gitian-building-mac-os-sdk.md) to get the required SDK.

To build the most recent tag (pass in `--docker` if using docker):

```bash
./gitian-build.py --detach-sign --no-commit --build $GH_USER $VERSION
```

To speed up the build, use `-j 5 --memory 5000` as the first arguments, where `5` is the number of CPU's you allocated to the VM plus one, and 5000 is a little bit less than then the MB's of RAM you allocated. If there is memory corruption on your machine, try to tweak these values.

If all went well, this produces a number of (uncommitted) `.assert` files in the gitian.sigs directory.

Checking your work
------------------

Take a look in the assert files and note the SHA256 checksums listed there. eg for `v0.14.1.0` you should get this checksum:

```
2b95118f53d98d542a85f8732b84ba13b3cd20517ccb40332b0edd0ddf4f8c62  monero-x86_64-linux-gnu.tar.gz
```

You should verify that this is really the checksum you get on that file you built.  You can also look in the gitian.sigs repo and / or [getmonero.org release checksums](https://web.getmonero.org/downloads/hashes.txt) to see if others got the same checksum for the same version tag.  If there is ever a mismatch -- **STOP! Something is wrong**.  Contact others on IRC / github to figure out what is going on.


Signing assert files
--------------------

If you chose to do detached signing using `--detach-sign` above (recommended), you need to copy these uncommitted changes to your host machine, then sign them using your gpg key like so:

```bash
GH_USER=fluffypony
VERSION=v0.14.1.0

gpg --detach-sign ${VERSION}-linux/${GH_USER}/monero-linux-*-build.assert
gpg --detach-sign ${VERSION}-win/${GH_USER}/monero-win-*-build.assert
gpg --detach-sign ${VERSION}-osx/${GH_USER}/monero-osx-*-build.assert
```
<!-- TODO: Replace * above with ${VERSION} once gitian builds correct file name -->

This will create a `.sig` file for each `.assert` file above (2 files for each platform).


Submitting your signed assert files
-----------------------------------

Make a pull request (both the `.assert` and `.assert.sig` files) to the
[monero-project/gitian.sigs](https://github.com/monero-project/gitian.sigs/) repository:

```bash
git checkout -b $VERSION
# add your assert and sig files...
git commit -S -a -m "Add $GH_USER $VERSION"
git push --set-upstream $GH_USER $VERSION
```

**Note:** Please ensure your gpg public key is available to check signatures by adding it to the [gitian.sigs/gitian-pubkeys/](https://github.com/monero-project/gitian.sigs/tree/master/gitian-pubkeys) directory in a pull request.


More Build Options
------------------

You can choose your own remote and commit hash by running for example:
```bash
./gitian-build.py --detach-sign --no-commit --url https://github.com/moneromooo-monero/bitmonero -b moneromooo 1f5680c8db8f4cc7acc04a04c724b832003440fd
```

Note that you won't be able to build commits authored before the gitian scripts
were added. Gitian clones the source files from the given url, be sure to push
to the remote you provide before building.
To get all build options run:
```bash
./gitian-build.py --help
```

