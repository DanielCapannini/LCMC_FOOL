push 0
push 4
push 5
bleq label10
push 0
b label11
label10:
push 1
label11:
lfp
push -2
add
lw
print
halt