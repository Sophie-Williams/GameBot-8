package me.james.gamebot.cah;

import java.util.*;

public class CAHPack
{
    private String id, name;
    private ArrayList<CAHBlackCard> blackCards = new ArrayList<>();
    private ArrayList<CAHWhiteCard> whiteCards = new ArrayList<>();

    public CAHPack( String id, String name )
    {
        this.id = id;
        this.name = name;
    }

    public String getID()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public void addBlackCard( CAHBlackCard card )
    {
        blackCards.add( card );
    }

    public void addWhiteCard( CAHWhiteCard card )
    {
        whiteCards.add( card );
    }

    public CAHBlackCard[] getBlackCards()
    {
        return blackCards.toArray( new CAHBlackCard[blackCards.size()] );
    }

    public CAHWhiteCard[] getWhiteCards()
    {
        return whiteCards.toArray( new CAHWhiteCard[whiteCards.size()] );
    }
}
