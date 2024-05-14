push 0
push 0
push 1
beq label24
push 1
push 1
beq label24
push 0
b label25
label24:
push 1
label25:
lfp
push -2
add
lw
print
halt