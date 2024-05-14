push 0
push 1
push 1
beq label12
push 0
push 1
beq label12
push 0
b label13
label12:
push 1
label13:
lfp
push -2
add
lw
print
halt