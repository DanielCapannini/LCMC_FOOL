push 0
push 1
push 0
beq label16
push 1
push 0
beq label16
push 1
b label17
label16:
push 0
label17:
lfp
push -2
add
lw
print
halt