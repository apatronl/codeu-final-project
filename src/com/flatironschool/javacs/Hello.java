import static com.lexicalscope.jewel.cli.CliFactory.parseArguments;
import com.lexicalscope.jewel.cli.ArgumentValidationException;

public class Hello {
    public static void main(String [] args) {
        try {
            Person person = parseArguments(Person.class, args);
            for (int i = 0; i < person.getTimes(); i++)
                System.out.println("Hello " +  person.getName());
        } catch(ArgumentValidationException e) {
            System.err.println(e.getMessage());
        }
    }
}
