push 0
push 0
push 0
beq label2
push 1
push 0
beq label2
push 1
b label3
label2:
push 0
label3:
lfp
push -2
add
lw
print
halt