package me.james.gamebot.cah;

import com.google.gson.*;
import java.io.*;
import java.util.*;
import me.james.gamebot.*;
import sx.blah.discord.api.events.*;
import sx.blah.discord.handle.impl.events.*;
import sx.blah.discord.util.*;

public class CardsAgainstHumanity
{
    public static final long ROUND_TIME = 90000L;
    private static HashMap<String, CAHPack> packs = new HashMap<>();
    private static HashMap<String, CAHGame> games = new HashMap<>();

    public static CAHPack[] getPacks()
    {
        return packs.values().toArray( new CAHPack[packs.size()] );
    }

    public static CAHGame[] getGames()
    {
        return games.values().toArray( new CAHGame[games.values().size()] );
    }

    public static CAHPack getPack( String id )
    {
        return packs.get( id );
    }

    public static CAHWhiteCard[] getWhiteCardsFromPacks( CAHPack[] packs )
    {
        ArrayList<CAHWhiteCard> whites = new ArrayList<>();
        for ( CAHPack p : packs )
        {
            for ( CAHWhiteCard wCard : p.getWhiteCards() )
            {
                whites.add( wCard );
            }
        }
        return whites.toArray( new CAHWhiteCard[whites.size()] );
    }

    public static CAHBlackCard[] getBlackCardsFromPacks( CAHPack[] packs )
    {
        ArrayList<CAHBlackCard> blacks = new ArrayList<>();
        for ( CAHPack p : packs )
        {
            for ( CAHBlackCard bCard : p.getBlackCards() )
            {
                blacks.add( bCard );
            }
        }
        return blacks.toArray( new CAHBlackCard[blacks.size()] );
    }

    public static CAHGame getGame( String id )
    {
        return games.get( id );
    }

    public static void init()
    {
        GameBot.getLogger().info( "Initializing CAH..." );
        for ( File f : new File( "cahcards" ).listFiles() )
        {
            if ( !f.isFile() )
                continue;
            JsonObject packFile = GameBot.fileToJSON( f.getPath() );
            String id = packFile.get( "order" ).getAsJsonArray().get( 0 ).getAsString();
            CAHPack pack = new CAHPack( id, packFile.get( id ).getAsJsonObject().get( "name" ).getAsString() );
            packFile.get( "blackCards" ).getAsJsonArray().forEach( elem -> pack.addBlackCard( new CAHBlackCard( elem.getAsJsonObject().get( "text" ).getAsString(), elem.getAsJsonObject().get( "pick" ).getAsInt() ) ) );
            packFile.get( "whiteCards" ).getAsJsonArray().forEach( elem -> pack.addWhiteCard( new CAHWhiteCard( elem.getAsString(), false ) ) );
            packs.put( pack.getID(), pack );
            GameBot.getLogger().info( "Registered CAH card pack '" + pack.getName() + "' (" + pack.getID() + "), containing " + pack.getBlackCards().length + " black cards and " + pack.getWhiteCards().length + " white cards." );
        }
        int totalbCards = 0;
        int totalwCards = 0;
        for ( CAHPack p : packs.values() )
        {
            totalbCards = totalbCards + p.getBlackCards().length;
            totalwCards = totalwCards + p.getWhiteCards().length;
        }
        GameBot.getLogger().info( "There are a total of " + totalbCards + " black cards and " + totalwCards + " white cards within ALL packs." );
        GameBot.getLogger().info( "CAH Initialized." );
    }

    public static CAHGame createGame( String owner )
    {
        CAHGame game = new CAHGame( owner );
        games.put( game.getID(), game );
        GameBot.getLogger().info( "Game " + game.getID() + " created by " + game.getOwnerID() + "." );
        return game;
    }

    public static void removeGame( String id )
    {
        games.remove( id );
    }

    public static CAHGame getGameByID( String id )
    {
        return games.get( id );
    }

    public static CAHGame getGameFromUserID( String id )
    {
        for ( CAHGame g : games.values() )
        {
            if ( Arrays.asList( g.getPlayers() ).contains( id ) || g.getOwnerID().equals( id ) )
                return g;
        }
        return null;
    }

    @EventSubscriber
    public void onMessageReceive( MessageReceivedEvent e )
    {
        if ( e.getMessage().getContent().startsWith( "$" ) )
        {
            String[] args = e.getMessage().getContent().split( " " );
            if ( e.getMessage().getContent().startsWith( "$cah" ) )
            {
                if ( args.length <= 1 )
                {
                    try
                    {
                        e.getMessage().getChannel().sendMessage( "**CAH Commands:**\n**$cah creategame** - Create a game.\n**$cah leavegame** - Leaves your current game. (if an owner, disbands the current game)\n**$cah joingame <id>** - Join another users game.\n**$cah startgame** - Starts your game. (only usable by game creators)\n**$cah addpack <pack id>** - Adds a CAH pack to your game. (only usable by game creators)\n**$cah removepack <pack id>** - Removes a CAH pack from your game. (only usable by game creators)\n**$cah packs** - Print the valid pack ids included.\n**$cah hand** - Lists your current hand. (only usable in game)\n**$cah play <card #> [blank card text...]** - Plays a current card in your hand by it's number. (only usable in game)\n**$cah select <id>** - Selects a card as the round winner (only usable by czar)" );
                    } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                    {
                        e1.printStackTrace();
                    }
                    return;
                }
                if ( args[1].equalsIgnoreCase( "creategame" ) )
                {
                    if ( CardsAgainstHumanity.getGameFromUserID( e.getMessage().getAuthor().getID() ) != null )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** You already are in a game." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    CAHGame g = CardsAgainstHumanity.createGame( e.getMessage().getAuthor().getID() );
                    try
                    {
                        e.getMessage().getChannel().sendMessage( "**Success:** Created game " + g.getID() + ".\nTell other users to use `$cah joingame " + g.getID() + "` to join your game!" );
                    } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                    {
                        e1.printStackTrace();
                    }
                } else if ( args[1].equalsIgnoreCase( "joingame" ) )
                {
                    if ( args.length < 3 )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** Usage: $cah joingame <gane id>" );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }

                    if ( CardsAgainstHumanity.getGameFromUserID( e.getMessage().getAuthor().getID() ) != null )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** You are already in a game. (" + CardsAgainstHumanity.getGameFromUserID( e.getMessage().getAuthor().getID() ).getID() + ")" );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }

                    CAHGame g = CardsAgainstHumanity.getGameByID( args[2] );
                    if ( g == null )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** Invalid game ID." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    if ( g.isStarted() )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** You cannot join a game in-progress." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    try
                    {
                        g.addUser( e.getMessage().getAuthor().getID() );
                    } catch ( DiscordException | MissingPermissionsException e1 )
                    {
                        e1.printStackTrace();
                    }
                } else if ( args[1].equalsIgnoreCase( "leavegame" ) )
                {
                    CAHGame g = CardsAgainstHumanity.getGameFromUserID( e.getMessage().getAuthor().getID() );
                    if ( g == null )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** You are not in a game." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }

                    if ( g.getOwnerID().equals( e.getMessage().getAuthor().getID() ) )
                    {
                        g.disbandGame();
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Success:** Disbanded game `" + g.getID() + "`." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    try
                    {
                        g.kickUser( e.getMessage().getAuthor().getID(), "Left game." );
                    } catch ( RateLimitException | DiscordException | MissingPermissionsException e1 )
                    {
                        e1.printStackTrace();
                    }
                } else if ( args[1].equalsIgnoreCase( "startgame" ) )
                {
                    CAHGame g = CardsAgainstHumanity.getGameFromUserID( e.getMessage().getAuthor().getID() );
                    if ( g == null )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** You are not in a game." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }

                    if ( !g.getOwnerID().equals( e.getMessage().getAuthor().getID() ) )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** You are not the game owner." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }

                    if ( g.isStarted() )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** Game already started!" );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }

                    if ( g.getPlayers().length < 3 )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** You need at least three people to start a game." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    if ( g.getPacks().length == 0 )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** You must add at least one pack.\nUse `$cah addpack <id>` to add a pack, using `$cah packs` to list all valid packs." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    try
                    {
                        g.startGame();
                    } catch ( DiscordException | MissingPermissionsException e1 )
                    {
                        e1.printStackTrace();
                    }
                } else if ( args[1].equalsIgnoreCase( "addpack" ) )
                {
                    if ( args.length < 3 )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** Usage: `$cah addpack <packid>`\nGet a list of packs using `$cah packs`." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    CAHGame g = CardsAgainstHumanity.getGameFromUserID( e.getMessage().getAuthor().getID() );
                    CAHPack p = CardsAgainstHumanity.getPack( args[2] );
                    if ( g == null )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** You are not in a game." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    if ( !g.getOwnerID().equals( e.getMessage().getAuthor().getID() ) )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** You are not the owner of the game." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    if ( p == null )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** Invalid pack id." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    g.addPack( p );
                } else if ( args[1].equalsIgnoreCase( "removepack" ) )
                {
                    if ( args.length < 3 )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** Usage: `$cah addpack <packid>`\nGet a list of packs using `$cah packs`." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    CAHGame g = CardsAgainstHumanity.getGameFromUserID( e.getMessage().getAuthor().getID() );
                    CAHPack p = CardsAgainstHumanity.getPack( args[2] );
                    if ( g == null )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** You are not in a game." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    if ( !g.getOwnerID().equals( e.getMessage().getAuthor().getID() ) )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** You are not the owner of the game." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    if ( p == null )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** Invalid pack id." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    g.removePack( p );
                } else if ( args[1].equalsIgnoreCase( "packs" ) )
                {
                    String msg = "**Valid Packs:**\n";
                    for ( CAHPack p : CardsAgainstHumanity.getPacks() )
                    {
                        msg += "- " + p.getID() + " **(" + p.getName() + ")**\n";
                    }
                    try
                    {
                        e.getMessage().getChannel().sendMessage( msg );
                    } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                    {
                        e1.printStackTrace();
                    }
                } else if ( args[1].equalsIgnoreCase( "hand" ) )
                {
                    CAHGame g = CardsAgainstHumanity.getGameFromUserID( e.getMessage().getAuthor().getID() );
                    if ( g == null )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** You are not in a game." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    if ( !g.isStarted() )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** The game has not started." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    CAHHand hand = g.getHand( e.getMessage().getAuthor().getID() );
                    if ( hand == null )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** You have no available hand." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    String msg = "**Your hand:**\n";
                    int count = 0;
                    for ( CAHWhiteCard wc : hand.getHand() )
                    {
                        msg += "**" + count + ":** " + wc.getText() + "\n";
                        count++;
                    }
                    msg += "Use `$cah play <card #>` to play one of your cards.";
                    try
                    {
                        e.getMessage().getChannel().sendMessage( msg );
                    } catch ( MissingPermissionsException | DiscordException | RateLimitException e1 )
                    {
                        e1.printStackTrace();
                    }
                } else if ( args[1].equalsIgnoreCase( "play" ) )
                {
                    if ( args.length < 3 )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** Usage: `$cah play <card #>`\nYou can get a card number from your hand using `$cah hand`." );
                        } catch ( MissingPermissionsException | DiscordException | RateLimitException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    CAHGame g = CardsAgainstHumanity.getGameFromUserID( e.getMessage().getAuthor().getID() );
                    if ( g == null )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** You must be in a game." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    if ( !g.isStarted() )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** The game has not started yet." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    if ( g.getCardCzarID().equals( e.getMessage().getAuthor().getID() ) )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** You are the card czar. Use `$cah select <id>` when everyone has played a card to select a winning card." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    int sel = 0;
                    try
                    {
                        sel = Integer.parseInt( args[2] );
                    } catch ( NumberFormatException ex )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** Invalid input." );
                            return;
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                    }
                    if ( sel < 0 || sel > g.getHand( e.getMessage().getAuthor().getID() ).getHand().length )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** Invalid card selection." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    if ( g.getPlayedCards( e.getMessage().getAuthor().getID() ).getPlayedCards().length == g.getRoundBlackCard().getPicks() )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** You cannot play another card." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    CAHWhiteCard card = g.getHand( e.getMessage().getAuthor().getID() ).getHand()[sel];
                    if ( card.isBlank() )
                    {
                        if ( args.length < 4 )
                        {
                            try
                            {
                                e.getMessage().getChannel().sendMessage( "**Error:** Playing a blank card requires the following syntax: `$cah play <blank #> <text...>" );
                            } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                            {
                                e1.printStackTrace();
                            }
                            return;
                        }
                        String text = "";
                        for ( int i = 3; i < args.length; i++ )
                        {
                            text += args[i] + " ";
                        }
                        card.setBlankText( text.substring( 0, text.length() - 1 ), e.getMessage().getAuthor().getID() );
                    }
                    g.playWhiteCard( e.getMessage().getAuthor().getID(), card );
                    try
                    {
                        e.getMessage().getChannel().sendMessage( "**Success:** You played a card. You need to play *" + ( g.getRoundBlackCard().getPicks() - g.getPlayedCards( e.getMessage().getAuthor().getID() ).getPlayedCards().length ) + "* more cards." );
                    } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                    {
                        e1.printStackTrace();
                    }
                } else if ( args[1].equalsIgnoreCase( "select" ) )
                {
                    if ( args.length < 3 )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** Usage: `$cah select <id>" );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    CAHGame g = CardsAgainstHumanity.getGameFromUserID( e.getMessage().getAuthor().getID() );
                    if ( g == null )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** You aren't in a game." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    if ( !g.isStarted() )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** The game has not started yet." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    if ( !g.getCardCzarID().equals( e.getMessage().getAuthor().getID() ) )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** You are not the czar." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    int sel = 0;
                    try
                    {
                        sel = Integer.parseInt( args[2] );
                    } catch ( NumberFormatException ex )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** Invalid input." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    g.selectWinner( sel );
                } else if ( args[1].equalsIgnoreCase( "broadcast" ) )
                {
                    if ( args.length < 3 )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** Usage: `$cah broadcast <message...>" );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    CAHGame g = CardsAgainstHumanity.getGameFromUserID( e.getMessage().getAuthor().getID() );
                    if ( g == null )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** You are not in a game." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    if ( !g.getOwnerID().equals( e.getMessage().getAuthor().getID() ) )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** You are not the owner of the game." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    String msg = "";
                    for ( int i = 2; i < args.length; i++ )
                    {
                        msg += args[i] + " ";
                    }
                    msg = msg.substring( 0, msg.length() - 1 );
                    try
                    {
                        g.sendGameMessage( msg );
                    } catch ( RateLimitException | DiscordException | MissingPermissionsException e1 )
                    {
                        e1.printStackTrace();
                    }
                } else if ( args[1].equalsIgnoreCase( "blanks" ) )
                {
                    if ( args.length < 3 )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** Usage: `$cah blanks <count>`" );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    CAHGame g = CardsAgainstHumanity.getGameFromUserID( e.getMessage().getAuthor().getID() );
                    if ( g == null )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** You are not in a game." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                    try
                    {
                        g.setBlanks( Integer.parseInt( args[2] ) );
                    } catch ( NumberFormatException ex )
                    {
                        try
                        {
                            e.getMessage().getChannel().sendMessage( "**Error:** Invalid input." );
                        } catch ( MissingPermissionsException | RateLimitException | DiscordException e1 )
                        {
                            e1.printStackTrace();
                        }
                        return;
                    }
                }
                //ADMIN COMMANDS
                try
                {
                    if ( e.getMessage().getAuthor().getID().equals( GameBot.getBot().getApplicationOwner().getID() ) )
                    {
                        if ( args[1].equalsIgnoreCase( "giveblank" ) )
                        {
                            CAHGame g = CardsAgainstHumanity.getGameFromUserID( e.getMessage().getAuthor().getID() );
                            if ( g == null )
                            {
                                e.getMessage().getChannel().sendMessage( "**Error:** You are not in a game." );
                                return;
                            }
                            if ( !g.isStarted() )
                            {
                                e.getMessage().getChannel().sendMessage( "**Error:** The game has not started." );
                                return;
                            }
                            g.getHand( e.getMessage().getAuthor().getID() ).addCard( new CAHWhiteCard( "[Blank Card]", true ) );
                            e.getMessage().getChannel().sendMessage( "**Success:** Your hand now contains a blank card." );
                        } else if ( args[1].equalsIgnoreCase( "whohas" ) )
                        {
                            if ( args.length < 3 )
                            {
                                e.getMessage().getChannel().sendMessage( "**Error:** Usage: `$cah whohas <partial card text...>" );
                                return;
                            }
                            CAHGame g = CardsAgainstHumanity.getGameFromUserID( e.getMessage().getAuthor().getID() );
                            if ( g == null )
                            {
                                e.getMessage().getChannel().sendMessage( "**Error:** You are not in a game." );
                                return;
                            }
                            if ( !g.isStarted() )
                            {
                                e.getMessage().getChannel().sendMessage( "**Error:** The game has not started." );
                                return;
                            }
                            String textToContain = "";
                            for ( int i = 2; i < args.length; i++ )
                            {
                                textToContain += args[i] + " ";
                            }
                            textToContain = textToContain.substring( 0, textToContain.length() - 1 );
                            for ( CAHHand h : g.getHands() )
                            {
                                for ( CAHWhiteCard wc : h.getHand() )
                                {
                                    if ( wc.getText().contains( textToContain ) )
                                    {
                                        e.getMessage().getChannel().sendMessage( "**Success:** User " + GameBot.getBot().getUserByID( h.getID() ).getName() + " (" + h.getID() + ") has that card. *('" + wc.getText() + ")*" );
                                        return;
                                    }
                                }
                            }
                            e.getMessage().getChannel().sendMessage( "**Error:** No user has that card." );
                        }
                    }
                } catch ( DiscordException | RateLimitException | MissingPermissionsException e1 )
                {
                    e1.printStackTrace();
                }
            }
        }
    }
}
