package tr.com.orsan.academy.learning.netty.sbe.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.agrona.concurrent.UnsafeBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tr.com.orsan.academy.learning.netty.sbe.messages.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class SimpleServerMain extends ChannelInboundHandlerAdapter {
    private static final String ENCODING_FILENAME = "sbe.encoding.filename";
    private static final byte[] VEHICLE_CODE;
    private static final byte[] MANUFACTURER_CODE;
    private static final byte[] MANUFACTURER;
    private static final byte[] MODEL;
    private static final UnsafeBuffer ACTIVATION_CODE;

    static {
        try {
            VEHICLE_CODE = "abcdef".getBytes(CarEncoder.vehicleCodeCharacterEncoding());
            MANUFACTURER_CODE = "123".getBytes(EngineEncoder.manufacturerCodeCharacterEncoding());
            MANUFACTURER = "Honda".getBytes(CarEncoder.manufacturerCharacterEncoding());
            MODEL = "Civic VTi".getBytes(CarEncoder.modelCharacterEncoding());
            ACTIVATION_CODE = new UnsafeBuffer("abcdef".getBytes(CarEncoder.activationCodeCharacterEncoding()));
        } catch (final UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }


    private static final Logger logger = LogManager.getLogger(SimpleServerMain.class);
    private int port;
    private int[][] myInputArr;


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
        final ByteBuffer byteBuffer = ByteBuffer.allocate(4096);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        final MessageHeaderDecoder messageHeaderDecoder = new MessageHeaderDecoder();
        final MessageHeaderEncoder messageHeaderEncoder = new MessageHeaderEncoder();
        final CarDecoder carDecoder = new CarDecoder();
        final CarEncoder carEncoder = new CarEncoder();

        final int encodingLengthPlusHeader = encode(carEncoder, directBuffer, messageHeaderEncoder);

        final ByteBuf nettyBuffer = ctx.alloc().buffer(encodingLengthPlusHeader); // (2)

        byteBuffer.limit(encodingLengthPlusHeader);
        nettyBuffer.writeBytes(byteBuffer);

        final ChannelFuture f = ctx.writeAndFlush(nettyBuffer); // (3)
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


    int encode(
            final CarEncoder car, final UnsafeBuffer directBuffer, final MessageHeaderEncoder messageHeaderEncoder) {
        car.wrapAndApplyHeader(directBuffer, 0, messageHeaderEncoder)
                .serialNumber(1234)
                .modelYear(2013)
                .available(BooleanType.T)
                .code(Model.A)
                .putVehicleCode(VEHICLE_CODE, 0);

        car.putSomeNumbers(1, 2, 3, 4);

        car.extras()
                .clear()
                .cruiseControl(true)
                .sportsPack(true)
                .sunRoof(false);

        car.engine()
                .capacity(2000)
                .numCylinders((short) 4)
                .putManufacturerCode(MANUFACTURER_CODE, 0)
                .efficiency((byte) 35)
                .boosterEnabled(BooleanType.T)
                .booster().boostType(BoostType.NITROUS).horsePower((short) 200);

        car.fuelFiguresCount(3)
                .next().speed(30).mpg(35.9f).usageDescription("Urban Cycle")
                .next().speed(55).mpg(49.0f).usageDescription("Combined Cycle")
                .next().speed(75).mpg(40.0f).usageDescription("Highway Cycle");

        final CarEncoder.PerformanceFiguresEncoder figures = car.performanceFiguresCount(2);
        figures.next()
                .octaneRating((short) 95)
                .accelerationCount(3)
                .next().mph(30).seconds(4.0f)
                .next().mph(60).seconds(7.5f)
                .next().mph(100).seconds(12.2f);
        figures.next()
                .octaneRating((short) 99)
                .accelerationCount(3)
                .next().mph(30).seconds(3.8f)
                .next().mph(60).seconds(7.1f)
                .next().mph(100).seconds(11.8f);

        // An exception will be raised if the string length is larger than can be encoded in the varDataEncoding field
        // Please use a suitable schema type for varDataEncoding.length: uint8 <= 254, uint16 <= 65534
        car.manufacturer(new String(MANUFACTURER, StandardCharsets.UTF_8))
                .putModel(MODEL, 0, MODEL.length)
                .putActivationCode(ACTIVATION_CODE, 0, ACTIVATION_CODE.capacity());

        return MessageHeaderEncoder.ENCODED_LENGTH + car.encodedLength();
    }
}
