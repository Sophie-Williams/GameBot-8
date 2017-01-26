package me.james.gamebot.cah;

import me.james.gamebot.*;

public class CAHWhiteCard
{
    private String text;
    private boolean isBlank;

    public CAHWhiteCard( String text, boolean isBlank )
    {
        this.isBlank = isBlank;
        if ( !this.isBlank )
            this.text = text;
    }

    public String getText()
    {
        return text;
    }

    public void setBlankText( String text, String who )
    {
        if ( !isBlank() )
            return;
        GameBot.getLogger().info( "User " + GameBot.getBot().getUserByID( who ).getName() + " (" + who + ") used a blank card, it's text was: '" + text + "'." );
        this.text = text;
    }

    public boolean isBlank()
    {
        return isBlank;
    }
}
