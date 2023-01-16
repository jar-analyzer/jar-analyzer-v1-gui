## 0.1-beta

nothing

## 0.2-beta

Bugs:
- [bug] right click on mac os not work #5
- [bug] mac os open too many files bug #6

Others:
- [new feature] jars in jar file situation #1
- [new feature] allow user input a jar directory #3

## 0.3-beta

Congratulates, I make some meaningful updates. Please note, run this on Java 11-17, other versions not support, 
and I had not test this on Mac OS and Linux, if you found a bug, please report a issue, much thanks. 
(In fact, I can chose a lower version of QuiltFlower so that this can run under Java 8, 
but I wonder if it's worth it, is there many Java 8 users?)

Support 3 ways to decompile class file:
- QuiltFlower (FernFlower)
- Procyon
- CFR

Rebuild UI and add a progress bar showing the progress of loading jars.

Interestingly, I have added some example cases in the README.md.

Finally, I made an exe version, but you need have a local JRE 11-17 environment.

Important:
- Add CFR and QuiltFlower decompilers #8
- [docs] add a more complex example case #9
- [new feature] a progress bar to show load jar progress #4

Bugs:
- [bug] code panel cannot resize #10

Others:
- [new feature] allow use null class name and search all methods #7

## 0.4-beta

This is a buf fix version, does not contain new features.
So I had not made an exe version, only a jar version.

I try to make the cursor as accurate as possible to point to the real method call position.
But I can't confirm all the situations. If you find any problems, you can ask for issue.
And I chose a lower version of QuiltFlower to support Java 8.

Important:
- [important] [bug] method position not accurate enough #2
- [important] [bug] the number of parameters does not match show info #16

Others:
- use quilt flower 1.8.1 to support Java 8 #15
- remove unnecessary dependencies to reduce jar file size

## 0.5-beta

New Features:
- [important] [feat] add a new panel to show your decompile history #13
- [important] [feat] add a panel to show subclasses and super classes #11
- [important] [feat] add a option to chose search directly or method call #19
- [feat] show formatted bytecode and asm code #18

Others:
- use quilt flower 1.8.1 to support Java 8 #15 (support 8 and 11)

## 0.6-beta

This is a special version, which support a new feature but is not stable

Change:
- Rebuild UI
- Spring Controller and Mapping Analysis
- New README

## 0.7

Important version: https://www.bilibili.com/video/BV1Pe4y1T7Ma

Bug:
- [important] [bug] cannot decompile spring class sometimes #28
- [important] [bug] spring should not contain duplicate content #31
- [bug] springboot loader class npe bug #30
- [bug] macos tabbed panel bug #25

Feature:
- [important] [feat] prompt the user why decompilation fails #33
- [important] [feat] support getmapping and postmapping #23
- [important] [feat] controller without controller anno #24
- [feat] prompt user what is inner jar and spring boot jar #35
- [feat] prompt user when the search result is empty #34
- [feat] delete logs when exit checkbox #32

Others:
- [important] [improve] code refactor #27
- [improve] show number should be changed to details #29
- [improve] spring analyze use asm 9 #26
- spring package is unnecessary (delete ui)
- new README and screenshot

## 0.8

更新内容：
- [important] 整体UI使用中文重构（我不觉得老外会使用） #40
- [feat] 应该对两种搜索方式给出提示 #37
- [bug] CFR反编译有时候不工作 #36
- [improve] 当选择反编译类时应该清空方法调用信息 #39
