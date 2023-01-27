package tr.com.orsan.academy.learning.netty.sbe.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.agrona.concurrent.UnsafeBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tr.com.orsan.academy.learning.netty.sbe.messages.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SimpleNettySbeDecoder extends ByteToMessageDecoder {
    private static final Logger logger = LogManager.getLogger(SimpleNettySbeDecoder.class);

    protected SimpleNettySbeDecoder() {
        super();
        this.setSingleDecode(false);
    }
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {


        if (in.readableBytes() < MessageHeaderDecoder.ENCODED_LENGTH + CarDecoder.BLOCK_LENGTH) {
            ChannelFuture future = ctx.writeAndFlush(null);
            future.addListener(ChannelFutureListener.CLOSE);
            return;
        }
        final ByteBuffer byteBuffer = ByteBuffer.allocate(in.readableBytes());
        in.readBytes(byteBuffer);

        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);
        final MessageHeaderDecoder messageHeaderDecoder = new MessageHeaderDecoder();
        final CarDecoder carDecoder = new CarDecoder();
        final int bufferOffset = 0;
        messageHeaderDecoder.wrap(directBuffer, bufferOffset);
        if (messageHeaderDecoder.schemaId() != CarEncoder.SCHEMA_ID) {
            throw new IllegalStateException("Schema ids do not match");
        }
        // Lookup the applicable flyweight to decode this type of message based on templateId and version.
        final int templateId = messageHeaderDecoder.templateId();
        if (templateId != CarEncoder.TEMPLATE_ID) {
            throw new IllegalStateException("Template ids do not match");
        }
        try {
            decode(carDecoder, directBuffer, messageHeaderDecoder);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        out.add(carDecoder);

        ctx.channel().flush();
    }
    public static void decode(
            final CarDecoder car, final UnsafeBuffer directBuffer, final MessageHeaderDecoder headerDecoder)
            throws Exception {
        final byte[] buffer = new byte[128];
        final StringBuilder sb = new StringBuilder();

        car.wrapAndApplyHeader(directBuffer, 0, headerDecoder);
        sb.append("\ncar.serialNumber=").append(car.serialNumber());
        sb.append("\ncar.modelYear=").append(car.modelYear());
        sb.append("\ncar.available=").append(car.available());
        sb.append("\ncar.code=").append(car.code());

        sb.append("\ncar.someNumbers=");
        for (int i = 0, size = CarEncoder.someNumbersLength(); i < size; i++) {
            sb.append(car.someNumbers(i)).append(", ");
        }

        sb.append("\ncar.vehicleCode=");
        for (int i = 0, size = CarEncoder.vehicleCodeLength(); i < size; i++) {
            sb.append((char) car.vehicleCode(i));
        }

        final OptionalExtrasDecoder extras = car.extras();
        sb.append("\ncar.extras.cruiseControl=").append(extras.cruiseControl());
        sb.append("\ncar.extras.sportsPack=").append(extras.sportsPack());
        sb.append("\ncar.extras.sunRoof=").append(extras.sunRoof());

        sb.append("\ncar.discountedModel=").append(car.discountedModel());

        final EngineDecoder engine = car.engine();
        sb.append("\ncar.engine.capacity=").append(engine.capacity());
        sb.append("\ncar.engine.numCylinders=").append(engine.numCylinders());
        sb.append("\ncar.engine.maxRpm=").append(engine.maxRpm());
        sb.append("\ncar.engine.manufacturerCode=");
        for (int i = 0, size = EngineEncoder.manufacturerCodeLength(); i < size; i++) {
            sb.append((char) engine.manufacturerCode(i));
        }
        sb.append("\ncar.engine.efficiency=").append(engine.efficiency());
        sb.append("\ncar.engine.boosterEnabled=").append(engine.boosterEnabled());
        sb.append("\ncar.engine.booster.boostType=").append(engine.booster().boostType());
        sb.append("\ncar.engine.booster.horsePower=").append(engine.booster().horsePower());

        sb.append("\ncar.engine.fuel=").append(
                new String(buffer, 0, engine.getFuel(buffer, 0, buffer.length), StandardCharsets.US_ASCII));

        for (final CarDecoder.FuelFiguresDecoder fuelFigures : car.fuelFigures()) {
            sb.append("\ncar.fuelFigures.speed=").append(fuelFigures.speed());
            sb.append("\ncar.fuelFigures.mpg=").append(fuelFigures.mpg());
            sb.append("\ncar.fuelFigures.usageDescription=").append(fuelFigures.usageDescription());
        }

        for (final CarDecoder.PerformanceFiguresDecoder performanceFigures : car.performanceFigures()) {
            sb.append("\ncar.performanceFigures.octaneRating=").append(performanceFigures.octaneRating());

            for (final CarDecoder.PerformanceFiguresDecoder.AccelerationDecoder acceleration : performanceFigures.acceleration()) {
                sb.append("\ncar.performanceFigures.acceleration.mph=").append(acceleration.mph());
                sb.append("\ncar.performanceFigures.acceleration.seconds=").append(acceleration.seconds());
            }
        }

        sb.append("\ncar.manufacturer=").append(car.manufacturer());

        sb.append("\ncar.model=").append(
                new String(buffer, 0, car.getModel(buffer, 0, buffer.length), CarEncoder.modelCharacterEncoding()));

        final UnsafeBuffer tempBuffer = new UnsafeBuffer(buffer);
        final int tempBufferLength = car.getActivationCode(tempBuffer, 0, tempBuffer.capacity());
        sb.append("\ncar.activationCode=").append(
                new String(buffer, 0, tempBufferLength, CarEncoder.activationCodeCharacterEncoding()));

        sb.append("\ncar.encodedLength=").append(car.encodedLength());

        logger.debug(sb.toString());
    }


    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception { // (1)
        logger.debug("Client joined... - " + ctx.name());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("Client left.... -" + ctx.name());
        super.channelInactive(ctx);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        logger.debug("Handler added");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }


}
