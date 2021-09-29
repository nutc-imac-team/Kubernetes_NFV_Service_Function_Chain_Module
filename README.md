# Kubernetes_NFV_Service_Function_Chain_Module
## 環境
> ONOS:2.0.0 up  
ubuntu:16.04  
java:1.8.0  Cancel changes
## 安裝Java環境

### Install Openjdk jre
```shell=
apt-get update
apt-get install -y default-jre default-jdk
export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64
```
### 下載 onos
#### zip檔
[官方下載頁面](https://wiki.onosproject.org/display/ONOS/Downloads)
```shell=
wget https://repo1.maven.org/maven2/org/onosproject/onos-releases/2.0.0/onos-2.0.0.tar.gz
tar zxvf onos-2.0.0.tar.gz
screen ./onos-2.0.0/bin/onos-service
```
screen -ls
screen -r 進入
> control a+d 退出 screen
### build onos
不能在root模式執行
> https://wiki.onosproject.org/display/ONOS/Developer+Quick+Start

#### install python
```shell=
sudo apt install python
sudo apt install python3
sudo apt install python-pip python-dev python-setuptools
sudo apt install python3-pip python3-dev python3-setuptools
 
pip3 install --upgrade pip
pip3 install selenium
```
#### install Bazelisk
```shell=
wget https://github.com/bazelbuild/bazelisk/releases/download/v1.4.0/bazelisk-linux-amd64
chmod +x bazelisk-linux-amd64
sudo mv bazelisk-linux-amd64 /usr/local/bin/bazel
bazel version
```
#### install 相關套件
```shell=
sudo apt-get update && \
    apt-get install -y \
        git \
        zip \
        curl \
        unzip \
        bzip2
```
#### 編譯onos
```shell=
git clone https://gerrit.onosproject.org/onos
cd onos
bazel build onos --verbose_failures --sandbox_debug
bazel run onos-local -- clean debug
```
#### 發現的錯誤
putIfAbsent -> put
```shell=
vim core/store/dist/src/main/java/org/onosproject/store/meter/impl/DistributedMeterStore.java

public MeterStoreResult storeMeterFeatures{
...
try {
        meterFeatures.put(key, meterfeatures);
    } catch (StorageException e) {
        result = MeterStoreResult.fail(TIMEOUT);
    }
...
}
```
#### 登入頁面
http://<node ip>:8181/onos/ui/login.html  
>account:onos
 password:rocks
