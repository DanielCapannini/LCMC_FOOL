push 0
push 0
push 1
beq label42
push 0
push 1
beq label42
push 0
b label43
label42:
push 1
label43:
lfp
push -2
add
lw
print
halt

function0:
cfp
lra
lfp
lw
push -1
add
lw
stm
sra
pop
sfp
ltm
lra
js

function1:
cfp
lra
lfp
lw
push -2
add
lw
stm
sra
pop
sfp
ltm
lra
js

function2:
cfp
lra
lfp
lw
push -1
add
lw
stm
sra
pop
sfp
ltm
lra
js

function3:
cfp
lra
push 30000
lfp
lfp
push 1
add
lw
stm
ltm
ltm
lw
push 0
add
lw
js
lfp
lfp
push 1
add
lw
stm
ltm
ltm
lw
push 1
add
lw
js
add
bleq label32
push 0
b label33
label32:
push 1
label33:
push 1
beq label30
push -1
b label31
label30:

lfp
lfp
lw
push -1
add
lw
stm
ltm
ltm
lw
push 0
add
lw
js

lhp
sw
lhp
push 1
add
shp
push 9998
lw
lhp
sw
lhp
lhp
push 1
add
shp
label31:
stm
sra
pop
pop
sfp
ltm
lra
js

function4:
cfp
lra
push 20000
lfp
lfp
push 1
add
lw
stm
ltm
ltm
lw
push 0
add
lw
js
bleq label36
push 0
b label37
label36:
push 1
label37:
push 1
beq label34
push -1
b label35
label34:

lfp
lfp
lw
push -1
add
lw
stm
ltm
ltm
lw
push 0
add
lw
js
lfp
lfp
lw
push -1
add
lw
stm
ltm
ltm
lw
push 1
add
lw
js

lhp
sw
lhp
push 1
add
shp
lhp
sw
lhp
push 1
add
shp
push 9997
lw
lhp
sw
lhp
lhp
push 1
add
shp
label35:
stm
sra
pop
pop
sfp
ltm
lra
js