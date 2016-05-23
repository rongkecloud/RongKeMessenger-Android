# RongKeMessenger-Android
RongKeMessenger for Android（融科通Android端源码）

![云视互动](http://www.rongkecloud.com/skin/simple/img/logo-small.png)

[Home Page(官方主页)](http://www.rongkecloud.com) | [Doc(文档手册)](http://www.rongkecloud.com/download/rongketong/doc.zip) |  [CHANGELOG(更新历史)](https://github.com/rongkecloud/RongKeMessenger-Android/blob/master/CHANGELOG.md)

## 功能介绍
融科通是基于云视互动打造的前、后完全开源APP，除完善的APP框架和后台设计外，还涵盖了注册、登录、通讯录管理、单聊、群聊、音视频通话、多人语音聊天室等即时通讯互动功能，陆续融科通还会逐步推出朋友圈、附近的人、多媒体客服等高级功能，旨在帮助广大开发者能够基于融科通开源代码上最低成本快速实现自身的产品。

融科通下载支持的电子市场有：豌豆荚、360手机助手、应用宝、百度手机助手、91助手、安卓市场。

注意：如果用户想以融科通为基础构建自己的应用APP并上线时，需要您修改包名为自己的包名。

## 基于开源框架融科通开发App说明：

下载Android端融科通开源代码，将RongKeMessenger-Android导入您的开发工具中。

#### 1、修改AndroidManifest.xml <br>
在AndroidManifest.xml文件中将
```Java
<meta-data android:name="RKCLOUD_KEY" android:value="您应用App的客户端秘钥" /> //Java
```
中的value值改为您应用的客户端秘钥。

#### 2、修改HttpApi <br>
参考文档配置服务器部分配置好您的服务器端，并且保证服务器端正常运行，可修改包com.rongkecloud.test.tttp下的HttpApi文件配置信息：
修改ROOT_HOST_NAME对应内容为您应用服务器端域名；
修改ROOT_HOST_PORT对应内容为您应用服务器端端口号；
修改API_PATH对应内容为您应用服务器端各个接口的公共路径。

#### 3、修改包名 <br>
需将融科通开源代码的包名改为您应用App的包名。
若您修改了整个项目的包名，需保证com.rongkecloud.test.ui.widget包下的文件在xml文件中的引用包名保持一致。
若您修改了rkchat包里面的包名，需保证com.rongkecloud.chat.demo.ui.widget包下的文件在xml文件中的引用包名保持一致。

#### 4、修改关于页面 <br>
在res->layout目录下修改文件setting_about.xml为您应用App的关于界面。
若您只需修改该界面的内容，而不修改格式，可在com.comhkecloud.test.entity包下的Constants.java文件中修改相关配置信息，其余配置信息也可在此文件中修改。

[Service Agreement(云视互动开发者平台服务协议)](http://www.rongkecloud.com/tecinfo/28.html)

[![联系我们][contactImage]](http://kefu.rongkecloud.com/RKServiceClientWeb/index.html?ek=6f2683bb7f9b98aa09283fd8b47f4086aec37b56&ct=1&bg=3&gd=143)
[Contact us(联系我们)][serviceLink]

[contactImage]: http://www.rongkecloud.com/skin/simple/img/right/online.png "在线客服"
[serviceLink]: http://kefu.rongkecloud.com/RKServiceClientWeb/index.html?ek=6f2683bb7f9b98aa09283fd8b47f4086aec37b56&ct=1&bg=3&gd=143 "在线客服"
