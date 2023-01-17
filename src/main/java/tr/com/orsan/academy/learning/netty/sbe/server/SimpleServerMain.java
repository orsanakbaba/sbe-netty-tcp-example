package tr.com.orsan.academy.learning.netty.sbe.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SimpleServerMain extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LogManager.getLogger(SimpleServerMain.class);
    private int port;
    private int[][] myInputArr;

    //private KaitaiNettyTcpExample kaitaiNettyTcpExample = new KaitaiNettyTcpExample(new ByteBufferKaitaiStream());

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
        final ByteBuf time = ctx.alloc().buffer(4); // (2)
        time.writeInt((int) (System.currentTimeMillis() / 1000L + 2208988800L));

        final ChannelFuture f = ctx.writeAndFlush(time); // (3)
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                assert f == future;
                ctx.close();
            }
        }); // (4)
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) { // (2)
        // Discard the received data silently.
        ((ByteBuf) msg).release(); // (3)
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
                            ch.pipeline().addLast(this);
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)          // (5)
                    .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

            // Bind and start to accept incoming connections.

            ChannelFuture f = b.bind(this.port).sync(); // (7)

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
