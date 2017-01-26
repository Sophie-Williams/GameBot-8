package me.james.gamebot.cah;

import java.util.*;

public class CAHPlayedCards
{
    private final String id;
    private ArrayList<CAHWhiteCard> playedCards = new ArrayList<>();

    public CAHPlayedCards( String id )
    {
        this.id = id;
    }

    public void addCard( CAHWhiteCard card )
    {
        playedCards.add( card );
    }

    public void reset()
    {
        playedCards.clear();
    }

    public CAHWhiteCard[] getPlayedCards()
    {
        return playedCards.toArray( new CAHWhiteCard[playedCards.size()] );
    }

    public String getID()
    {
        return id;
    }
}
