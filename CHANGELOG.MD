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
- [feat] show formatted bytecode and asm code #18

Others:
- use quilt flower 1.8.1 to support Java 8 #15 (support 8 and 11)