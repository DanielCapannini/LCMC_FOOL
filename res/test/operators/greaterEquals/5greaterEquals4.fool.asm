push 0
push 4
push 5
bleq label4
push 0
b label5
label4:
push 1
label5:
lfp
push -2
add
lw
print
halt