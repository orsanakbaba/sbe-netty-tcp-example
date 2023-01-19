package tr.com.orsan.academy.learning.netty.sbe.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.agrona.concurrent.UnsafeBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tr.com.orsan.academy.learning.netty.sbe.messages.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;


public class SimpleNettySbeEncoder extends MessageToByteEncoder { // (1)


    private static final String ENCODING_FILENAME = "sbe.encoding.filename";
    private static final byte[] VEHICLE_CODE;
    private static final byte[] MANUFACTURER_CODE;
    private static final byte[] MANUFACTURER;
    private static final byte[] MODEL;
    private static final UnsafeBuffer ACTIVATION_CODE;
    private static final Logger logger = LogManager.getLogger(SimpleNettySbeEncoder.class);

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

    private ByteBuf buf;

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Exception :", cause);
        ctx.close();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(4096);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        final MessageHeaderEncoder messageHeaderEncoder = new MessageHeaderEncoder();

        final CarEncoder carEncoder = new CarEncoder();

        final int encodingLengthPlusHeader = encodeCar(carEncoder, directBuffer, messageHeaderEncoder);

        final ByteBuf nettyBuffer = ctx.alloc().buffer(encodingLengthPlusHeader); // (2)

        byteBuffer.limit(encodingLengthPlusHeader);
        nettyBuffer.writeBytes(byteBuffer);

        final ChannelFuture f = ctx.writeAndFlush(nettyBuffer); // (3)
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                assert f == future;
                logger.debug("Car message successfully sent by client and received from the server.");
                ctx.close();
            }
        }); // (4)
    }

    public int encodeCar(
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
