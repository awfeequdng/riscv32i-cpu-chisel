.global _start
_start:
	li x1, 5
	li x2, 2
	div x3,x1,x2
	mul x4,x3,x2
	rem x5,x1,x2
	nop
	nop
end:
	j end
	nop
	nop
	nop
	nop
	nop
	nop
	nop
	nop
	nop
	nop