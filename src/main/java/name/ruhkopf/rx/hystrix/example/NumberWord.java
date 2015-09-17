package name.ruhkopf.rx.hystrix.example;

public class NumberWord
{
	private final Long number;
	private final String word;

	public NumberWord(final Long number, final String word)
	{
		super();
		this.number = number;
		this.word = word;
	}

	public Long getNumber()
	{
		return number;
	}

	public String getWord()
	{
		return word;
	}
}
