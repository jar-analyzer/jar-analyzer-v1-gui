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

Others:
- use quilt flower 1.8.1 to support Java 8 #15