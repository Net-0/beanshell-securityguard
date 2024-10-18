package InheritanceTest;

public class MyClassC extends MyClassA {
    // public int getNum() { return 10; }
    protected int getNum2() {
        new MyClassA().getNum();

        return this.getNum();
    }
}
