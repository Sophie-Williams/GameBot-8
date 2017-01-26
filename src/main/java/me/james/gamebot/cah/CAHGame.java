package me.james.gamebot.cah;

import java.util.*;
import me.james.gamebot.*;
import sx.blah.discord.util.*;

public class CAHGame implements Runnable
{
    private final String id;
    private final String ownerid;
    private boolean started = false;
    private int blanks;
    private ArrayList<String> players = new ArrayList<>();
    private HashMap<String, CAHHand> hands = new HashMap<>();
    private Thread thread;
    private HashMap<String, Integer> scores = new HashMap<>();
    private int maxscore = 10;
    private boolean gameOver = false;
    private int roundWinnerSelID;
    private String gameWinnerID;
    private CAHBlackCard roundBlackCard;
    private HashMap<String, CAHPlayedCards> roundWhiteCards = new HashMap<>();
    private String czarId;
    private int czarSelId;
    private ArrayList<CAHPack> packs = new ArrayList<>();
    private ArrayList<CAHWhiteCard> whiteCards = new ArrayList<>();
    private ArrayList<CAHBlackCard> blackCards = new ArrayList<>();
    private long roundTimeout;

    public CAHGame( String ownerid )
    {
        this.ownerid = ownerid;
        this.id = UUID.randomUUID().toString().split( "-" )[0];
        //this.blanks = 3;
        this.thread = new Thread( this, "cahGame_" + getID() );
        try
        {
            addUser( getOwnerID() );
        } catch ( DiscordException | MissingPermissionsException e )
        {
            e.printStackTrace();
        }
    }

    public int getBlanks()
    {
        return blanks;
    }

    public void setBlanks( int blanks )
    {
        this.blanks = blanks;
        try
        {
            sendGameMessage( "There are now **" + blanks + "** in the game." );
        } catch ( RateLimitException | DiscordException | MissingPermissionsException e )
        {
            e.printStackTrace();
        }
    }

    public String[] getPlayers()
    {
        return players.toArray( new String[players.size()] );
    }

    public CAHHand[] getHands()
    {
        return hands.values().toArray( new CAHHand[hands.values().size()] );
    }

    public void playWhiteCard( String id, CAHWhiteCard card )
    {
        roundWhiteCards.get( id ).addCard( card );
    }

    public CAHPlayedCards getPlayedCards( String id )
    {
        return roundWhiteCards.get( id );
    }

    public int getMaxScore()
    {
        return this.maxscore;
    }

    public void setMaxScore( int score )
    {
        this.maxscore = score;
    }

    public boolean selectWinner( int sel )
    {
        if ( sel > roundWhiteCards.values().size() )
            return false;
        roundWinnerSelID = sel;
        return true;
    }

    public void addScore( String id )
    {
        scores.put( id, scores.get( id ) + 1 );
    }

    public void removeScore( String id )
    {
        scores.put( id, scores.get( id ) - 1 );
    }

    public int getScore( String id )
    {
        return scores.get( id );
    }

    public void addUser( String id ) throws DiscordException, MissingPermissionsException
    {
        players.add( id );
        hands.put( id, new CAHHand( id ) );
        scores.put( id, 0 );
        roundWhiteCards.put( id, new CAHPlayedCards( id ) );
        try
        {
            sendGameMessage( "User " + GameBot.getBot().getUserByID( id ).getName() + " joined." );
        } catch ( RateLimitException e )
        {
            e.printStackTrace();
        }
    }

    public void startGame() throws DiscordException, MissingPermissionsException
    {
        GameBot.getLogger().info( String.format( "Started game %s owned by %s with %d players.", id, id, players.size() + 1 ) );
        try
        {
            sendGameMessage( "Game started." );
        } catch ( RateLimitException e )
        {
            e.printStackTrace();
        }
        started = true;
        whiteCards = new ArrayList<>( Arrays.asList( CardsAgainstHumanity.getWhiteCardsFromPacks( packs.toArray( new CAHPack[packs.size()] ) ) ) );
        blackCards = new ArrayList<>( Arrays.asList( CardsAgainstHumanity.getBlackCardsFromPacks( packs.toArray( new CAHPack[packs.size()] ) ) ) );
        czarSelId = -1;
        if ( getBlanks() > 0 )
            for ( int i = 0; i < getBlanks(); i++ )
                whiteCards.add( new CAHWhiteCard( "[Blank Card]", true ) );
        thread.start();
    }

    public boolean isStarted()
    {
        return started;
    }

    public String getOwnerID()
    {
        return ownerid;
    }

    public String getID()
    {
        return id;
    }

    public void addPack( CAHPack pack )
    {
        packs.add( pack );
        try
        {
            sendGameMessage( "Pack **" + pack.getName() + "** has been added." );
        } catch ( RateLimitException | DiscordException | MissingPermissionsException e1 )
        {
            e1.printStackTrace();
        }
    }

    public void removePack( CAHPack pack )
    {
        packs.remove( pack );
        try
        {
            sendGameMessage( "Pack **" + pack.getName() + "** has been removed." );
        } catch ( RateLimitException | DiscordException | MissingPermissionsException e1 )
        {
            e1.printStackTrace();
        }
    }

    public CAHPack[] getPacks()
    {
        return packs.toArray( new CAHPack[packs.size()] );
    }

    public CAHHand getHand( String id )
    {
        return hands.get( id );
    }

    public void sendGameMessage( String text ) throws RateLimitException, DiscordException, MissingPermissionsException
    {
        for ( String userId : getPlayers() )
        {
            GameBot.getBot().getUserByID( userId ).getOrCreatePMChannel().sendMessage( text );
        }
    }

    public void drawAllRequiredCards()
    {
        Random r = new Random();
        for ( CAHHand h : hands.values() )
        {
            if ( h.getHand().length >= 10 )
                continue;
            for ( int i = h.getHand().length; i < 10; i++ )
            {
                h.addCard( whiteCards.remove( r.nextInt( whiteCards.size() ) ) );
            }
        }
    }

    public void disbandGame()
    {
        started = false;
        try
        {
            thread.join();
        } catch ( InterruptedException e )
        {
            e.printStackTrace();
        }
        GameBot.getLogger().info( String.format( "Game %s was disbanded.", getID() ) );
        try
        {
            for ( String id : getPlayers() )
            {
                kickUser( id, "The owner disbanded the game." );
            }
        } catch ( RateLimitException | DiscordException | MissingPermissionsException e )
        {
            e.printStackTrace();
        }
        CardsAgainstHumanity.removeGame( getID() );
    }

    public CAHBlackCard getRoundBlackCard()
    {
        return roundBlackCard;
    }

    public void kickUser( String id, String reason ) throws RateLimitException, DiscordException, MissingPermissionsException
    {
        if ( getOwnerID().equals( id ) ) //Owner of the game, must disband!
        {
            disbandGame();
            return;
        }
        try
        {
            sendGameMessage( "User " + GameBot.getBot().getUserByID( id ).getName() + " kicked. **(" + reason + ")**" );
        } catch ( RateLimitException e )
        {
            e.printStackTrace();
        }
        players.remove( id );
        hands.remove( id );
        roundWhiteCards.remove( id );
    }

    public String getCardCzarID()
    {
        return czarId;
    }

    @Override
    public void run()
    {
        while ( !gameOver )
        {
            if ( czarSelId >= getPlayers().length )
                czarSelId = 0;
            else
                czarSelId++;
            czarId = getPlayers()[czarSelId];
            roundBlackCard = blackCards.remove( new Random().nextInt( blackCards.size() ) );
            try
            {
                sendGameMessage( String.format( "**Card Czar for this round: **%s\n\n**Black card for this round:**\n`%s`", GameBot.getBot().getUserByID( czarId ).getName(), roundBlackCard.getText().replaceAll( "_", "___" ) ) );
            } catch ( RateLimitException | DiscordException | MissingPermissionsException e )
            {
                e.printStackTrace();
            }
            drawAllRequiredCards();
            roundTimeout = System.currentTimeMillis() + CardsAgainstHumanity.ROUND_TIME;
            while ( true )
            {
                if ( allUsersPlayed() || System.currentTimeMillis() >= roundTimeout )
                    break;
                try
                {
                    Thread.sleep( 1 );
                } catch ( InterruptedException e )
                {
                    e.printStackTrace();
                }
            }
            String finalMessage = "**White cards for this round:**\n\n";
            int count = 0;
            CAHPlayedCards[] pcarr = roundWhiteCards.values().toArray( new CAHPlayedCards[roundWhiteCards.values().size()] );
            for ( int i = 0; i < pcarr.length; i++ )
            {
                CAHPlayedCards pc = pcarr[i];
                if ( pc.getID().equals( getCardCzarID() ) )
                {
                    count++;
                    continue;
                }
                if ( pc.getPlayedCards().length < getRoundBlackCard().getPicks() ) //This person didn't play a card in time, kick em!
                {
                    try
                    {
                        kickUser( pc.getID(), String.format( "You did not play a card in time. (game `%s`)", getID() ) );
                        i--;
                    } catch ( RateLimitException | DiscordException | MissingPermissionsException e )
                    {
                        e.printStackTrace();
                    }
                    continue;
                }
                String finalWhite = "**" + count + ":** ";
                for ( CAHWhiteCard wc : pc.getPlayedCards() )
                {
                    finalWhite += wc.getText() + ", ";
                }
                finalWhite = finalWhite.substring( 0, finalWhite.length() - 2 );
                finalMessage += finalWhite + "\n";
                count++;
            }
            try
            {
                sendGameMessage( finalMessage );
            } catch ( RateLimitException | MissingPermissionsException | DiscordException e )
            {
                e.printStackTrace();
            }
            try
            {
                GameBot.getBot().getUserByID( getCardCzarID() ).getOrCreatePMChannel().sendMessage( "**Confirm a selection** using `$cah select <id>`." );
            } catch ( MissingPermissionsException | RateLimitException | DiscordException e )
            {
                e.printStackTrace();
            }
            roundWinnerSelID = -1;
            while ( roundWinnerSelID == -1 )
            {
                try
                {
                    Thread.sleep( 1 );
                } catch ( InterruptedException e )
                {
                    e.printStackTrace();
                }
            }
            CAHPlayedCards winner = roundWhiteCards.values().toArray( new CAHPlayedCards[roundWhiteCards.values().size()] )[roundWinnerSelID];
            addScore( winner.getID() );
            String msg = "Winner: " + GameBot.getBot().getUserByID( winner.getID() ).getName() + ", with cards:\n";
            for ( CAHWhiteCard wc : winner.getPlayedCards() )
            {
                msg += wc.getText() + " & ";
            }
            msg = msg.substring( 0, msg.length() - 3 );
            try
            {
                sendGameMessage( msg );
            } catch ( RateLimitException | DiscordException | MissingPermissionsException e )
            {
                e.printStackTrace();
            }
            for ( CAHPlayedCards pc : roundWhiteCards.values() )
            {
                for ( CAHWhiteCard wc : pc.getPlayedCards() )
                    whiteCards.add( wc );
            }
            for ( CAHPlayedCards pc : roundWhiteCards.values() )
            {
                for ( CAHWhiteCard wc : pc.getPlayedCards() )
                    whiteCards.add( wc );
                pc.reset();
            }
            blackCards.add( roundBlackCard );
            roundBlackCard = null;
            String[] scoreMsg = { "**Current scores:**\n" };
            scores.forEach( ( id, score ) -> scoreMsg[0] += "**" + GameBot.getBot().getUserByID( id ).getName() + ":** " + score + "\n" );
            try
            {
                sendGameMessage( scoreMsg[0] );
            } catch ( RateLimitException | DiscordException | MissingPermissionsException e )
            {
                e.printStackTrace();
            }
            try
            {
                Thread.sleep( 7500 );
            } catch ( InterruptedException e )
            {
                e.printStackTrace();
            }
            checkWin();
        }
        if ( gameWinnerID == null ) //Ended without winner
        {
            try
            {
                sendGameMessage( "Game ended without winner! Is this a bug, or was this intended?" );
                for ( String id : players )
                {
                    kickUser( id, "Game ended. (No winner; bug or intended?)" );
                }
            } catch ( RateLimitException | DiscordException | MissingPermissionsException e )
            {
                e.printStackTrace();
            }
            CardsAgainstHumanity.removeGame( getID() );
            return;
        }
        String winner = GameBot.getBot().getUserByID( gameWinnerID ).getName();
        try
        {
            sendGameMessage( "**WINNER:** " + winner );
        } catch ( RateLimitException | DiscordException | MissingPermissionsException e )
        {
            e.printStackTrace();
        }
        for ( String id : players )
        {
            try
            {
                kickUser( id, "Game ended." );
            } catch ( RateLimitException | DiscordException | MissingPermissionsException e )
            {
                e.printStackTrace();
            }
        }
        CardsAgainstHumanity.removeGame( getID() );
    }

    private boolean allUsersPlayed()
    {
        for ( CAHPlayedCards pc : roundWhiteCards.values() )
        {
            if ( getCardCzarID().equals( pc.getID() ) )
                continue;
            if ( pc.getPlayedCards().length < getRoundBlackCard().getPicks() )
                return false;
        }
        return true;
    }

    private void checkWin()
    {
        scores.forEach( ( id, score ) ->
        {
            if ( score >= getMaxScore() ) //Winner winner.
            {
                gameOver = true;
                gameWinnerID = id;
            }
        } );
    }
}
