package tr.com.orsan.academy.learning.netty.sbe.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class SimpleServerMain extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LogManager.getLogger(SimpleServerMain.class);
    private int port;
    private int[][] myInputArr;
    static final List<Channel> channels = new ArrayList<Channel>();


    public SimpleServerMain(int port, int numbRow, int numbColumn) {
        this.port = port;
        this.myInputArr = new int[numbRow][numbColumn];
        for (int i = 0; i < numbRow; i++) {
            for (int j = 0; j < numbColumn; j++) {
                this.myInputArr[i][j] = (i * numbRow) + j + 1;
            }
        }
        printMyArray(this.myInputArr, numbRow, numbColumn);
    }
    public static void printMyArray(int[][] ints, int numbRow, int numbColumn) {
        for (int i = 0; i < numbRow; i++) {
            for (int j = 0; j < numbColumn; j++) {
                logger.debug(ints[i][j] + ((j == numbColumn - 1 ? "\n" : ((j != numbColumn - 1) ? "," : ""))));
            }
        }
    }
    public static void main(String[] args) throws Exception {
        int port = 51444;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        new SimpleServerMain(port, 10, 10).run();
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) { // (1)

        logger.debug("Client joined - " + ctx);
        channels.add(ctx.channel());
    }

    private ByteBuf tmp;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        System.out.println("Handler added");
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        System.out.println("Handler removed");

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }

    public void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap(); // (2)
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) // (3)
                    .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new SimpleNettySbeDecoder())
                                    .addLast((ChannelInboundHandlerAdapter) this);
                        }
                    })
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .option(ChannelOption.SO_BACKLOG, 128)          // (5)
                    .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

            // Bind and start to accept incoming connections.

            ChannelFuture f = b.bind("localhost", this.port).sync(); // (7)
            logger.debug("Netty Server started.");

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }




}
