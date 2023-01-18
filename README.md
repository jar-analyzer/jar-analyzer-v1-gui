# Jar Analyzer
![](https://img.shields.io/badge/build-passing-brightgreen)
![](https://img.shields.io/badge/build-Java%2011-orange)
![](https://img.shields.io/github/downloads/4ra1n/jar-analyzer/total)
![](https://img.shields.io/github/v/release/4ra1n/jar-analyzer)
![](https://img.shields.io/badge/Java%20Code%20Lines-3505-orange)

## 介绍

没有英文文档，老外请自行翻译

一个用于分析`jar`包的GUI工具，尤其适合从事代码安全审计。可以在同时分析多个`jar`文件，可以轻易地搜索目标方法。
支持反编译字节码并自动构建类和方法之间的关系，帮助Java安全研究员更高效地工作。

[前往下载](https://github.com/4ra1n/jar-analyzer/releases/latest)

支持六种搜索方式：
- 直接根据类和方法名搜索（搜索定义）
- 根据方法调用搜索（该方法在哪里被调用）
- 搜索字符串（分析`LDC`指令找到精确位置）
- 正则搜索字符串（分析`LDC`指令找到精确位置）
- 无脑搜索（分析相关指令找到精确位置）
- 二进制搜索（直接从二进制里搜）

支持选择三项反编译方式：
- QuiltFlower (FernFlower变种，推荐方式)
- Procyon
- CFR

使用类定制化的`JSyntaxPane`组件（非官方）来展示`Java`代码

（在该库`https://code.google.com/archive/p/jsyntaxpane`的基础上加了很多黑科技）

![](img/001.png)

可以直接定位字符串（分析常量池相关指令实现精确定位）

![](img/003.png)

可以直接查看字节码

![](img/002.png)

可以直接分析`Spring`框架编写的项目

![](img/005.png)

## Quick Start

重要：请使用Java 11 - Java 17运行 （已提供内置`JRE`的`EXE`版本）

（测试在`Java 8`中可能有奇怪的`BUG`为了稳妥选择`Java 11`）

(1) 第一步：添加`jar`文件（支持单个`jar`文件和`jar`目录）
- 点击按钮 `Select Jar File` 打开jar文件
- 支持上传多个jar文件并且会在一起进行分析

请不要着急，分析jar文件需要花费少量的时间

注意：请等到进度条满时分析完成

(2) 第二步：输入你搜索的信息

我们支持三种格式的输入：
- `javax.naming.Context` (例如)
- `javax/naming/Context`
- `Context` (会搜索所有 `*.Context` 类)

提供了一种快速输入的方式

![](img/006.png)

二进制搜索只会返回是否存在，不会返回具体信息

![](img/004.png)

(3) 第三步：你可以双击进行反编译

游标将会精确地指向方法调用的位置

当反编译的过程中，方法之间的关系会被构建

在面板上的任何地方都可以双击进行反编译，并构建新的方法调用关系和展示

请注意：如果你遇到无法反编译的情况，你需要加载正确的jar文件

例如，我无法反编译`javax.naming.Context`因为我没有加入`rt.jar`文件，如果你加入了它，就可以正常反编译了

你可以使用`Ctrl+F`搜索代码和编辑

你可以单击任何一个选项，接下来将会显示方法的详细信息

你可以右键将选项发送到链中。你可以把链理解为一个收藏夹或记录。在链中你同样可以双击反编译，然后展示新的方法调用关系，或单机显示详情
如果链中某个选项是你不想要的，可以右键把该选项从链中删除

因此你可以构建出一个只属于你的调用链

`谁调用了当前方法` 和 `当前方法调用了谁` 中的所有方法调用关系同样可以双击反编译，单击看详情，右键加入链

## 关于

(1) 什么是方法之间的关系

```java
class Test{
    void a(){
        new Test().b();
    }
    
    void b(){
        Test.c();
    }
    
    static void c(){
        // code
    }
}
```

如果当前方法是 `b`

谁调用了当前方法: `Test` class `a` method

当前方法调用了谁: `Test` class `c` method

(2) 如何解决接口实现的问题

```java
class Demo{
    void demo(){
        new Test().test();
    }
}

interface Test {
    void test();
}

class Test1Impl implements Test {
    @Override
    public void test() {
        // code
    }
}

class Test2Impl implements Test {
    @Override
    public void test() {
        // code
    }
}
```

现在我们有 `Demo.demo -> Test.test` 数据, 但实际上它是 `Demo.demo -> TestImpl.test`.

因此我们添加了新的规则： `Test.test -> Test1Impl.test` 和 `Test.test -> Test2Impl.test`.

首先确保数据不会丢失，然后我们可以自行手动分析反编译的代码
- `Demo.demo -> Test.test`
- `Test.test -> Test1Impl.test`/`Test.test -> Test2Impl.test`

(3) 如何解决继承关系

```java
class Zoo{
    void run(){
        Animal dog = new Dog();
        dog.eat();
    }
}

class Animal {
    void eat() {
        // code
    }
}

class Dog extends Animal {
    @Override
    void eat() {
        // code
    }
}

class Cat extends Animal {
    @Override
    void eat() {
        // code
    }
}
```
`Zoo.run -> dog.cat` 的字节码是 `INVOKEVIRTUAL Animal.eat ()V`, 但我们只有这条规则 `Zoo.run -> Animal.eat`, 丢失了 `Zoo.run -> Dog.eat` 规则

这种情况下我们添加了新规则： `Animal.eat -> Dog.eat` 和 `Animal.eat -> Cat.eat`

首先确保数据不会丢失，然后我们可以自行手动分析反编译的代码
- `Zoo.run -> Animal.eat`
- `Animal.eat -> Dog.eat`/`Animal.eat -> Cat.eat`
