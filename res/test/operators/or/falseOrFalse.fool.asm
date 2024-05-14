push 0
push 0
push 1
beq label30
push 0
push 1
beq label30
push 0
b label31
label30:
push 1
label31:
lfp
push -2
add
lw
print
halt