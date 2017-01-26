package me.james.gamebot;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.logging.*;
import me.james.gamebot.cah.*;
import sx.blah.discord.api.*;
import sx.blah.discord.api.events.*;
import sx.blah.discord.handle.impl.events.*;
import sx.blah.discord.util.*;

public class GameBot
{
    private static final Logger LOG = Logger.getLogger( "GameBot" );
    private static IDiscordClient bot;

    public static void main( String[] args )
    {
        getLogger().setUseParentHandlers( false );
        getLogger().addHandler( new Handler()
        {
            @Override
            public void publish( LogRecord record )
            {
                if ( getFormatter() == null )
                {
                    setFormatter( new SimpleFormatter() );
                }

                try
                {
                    String message = getFormatter().format( record );
                    if ( record.getLevel().intValue() >= Level.WARNING.intValue() )
                    {
                        System.err.write( message.getBytes() );
                    } else
                    {
                        System.out.write( message.getBytes() );
                    }
                } catch ( Exception exception )
                {
                    reportError( null, exception, ErrorManager.FORMAT_FAILURE );
                }

            }

            @Override
            public void close() throws SecurityException {}

            @Override
            public void flush() {}
        } );
        getLogger().info( "Starting GameBot..." );
        try
        {
            login();
            bot.getDispatcher().registerListener( new GameBot() );
            bot.getDispatcher().registerListener( new CardsAgainstHumanity() );
        } catch ( DiscordException e )
        {
            e.printStackTrace();
        }
    }

    public static void login() throws DiscordException
    {
        bot = new ClientBuilder().withToken( getToken() ).login();
    }

    public static String getToken()
    {
        try
        {
            return Files.readAllLines( Paths.get( "E:\\client_token_gamebot" ) ).get( 0 );
        } catch ( IOException e )
        {
            getLogger().severe( "Unable to find token file, exiting..." );
            System.exit( -1 );
            e.printStackTrace();
        }
        return null;
    }

    public static Logger getLogger()
    {
        return LOG;
    }

    public static IDiscordClient getBot()
    {
        return bot;
    }

    public static JsonObject fileToJSON( String path )
    {
        try
        {
            return new JsonParser().parse( new FileReader( path ) ).getAsJsonObject();
        } catch ( FileNotFoundException e )
        {
            e.printStackTrace();
        }
        return null;
    }

    @EventSubscriber
    public void onReady( ReadyEvent e )
    {
        getLogger().info( "Discord sent ready event, starting up..." );
        CardsAgainstHumanity.init();
    }

}
