import com.lexicalscope.jewel.cli.Option;

public interface Person {
    @Option String getName();
    @Option int getTimes();
}
