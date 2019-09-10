package Classes.MetaClass;

public class Singleton {
    private static Singleton single_instance = new Singleton();

     public static Singleton getInstance(){
         return single_instance;
    }
}
