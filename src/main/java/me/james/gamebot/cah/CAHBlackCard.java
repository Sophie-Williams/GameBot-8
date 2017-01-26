package me.james.gamebot.cah;

public class CAHBlackCard
{
    private String text;
    private int pick;

    public CAHBlackCard( String text, int pick )
    {
        this.text = text;
        this.pick = pick;
    }

    public String getText()
    {
        return text;
    }

    public int getPicks()
    {
        return pick;
    }
}
