int pi = 3;
int N = 0xbabe;
int max;

int fib(int n) {
	if (n <= 0) return 0;
	else if (n == 1) return 1;
	else return fib(n-2) + fib(n-1);
}

int main() {
	int i = 0;
	int f;
	scan(max);
	pi = pi * 3;
	if (pi < max) {
		max = pi;
	}
	while(i < max) {
		f = fib(i);
		if (f < N) {
			print("fib",i,"=",f,"<",N);
		}
		else {
			print("fib",i,"=",f,">=",N);
		}
		i = i+1;
	}
	return 0;
}
