push 0
push 4
push 4
bleq label20
push 0
b label21
label20:
push 1
label21:
lfp
push -2
add
lw
print
halt