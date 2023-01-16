package tr.com.orsan.academy.learning.netty.sbe.client;

import io.kaitai.struct.ByteBufferKaitaiStream;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tr.com.orsan.academy.learning.netty.sbe.protocol.KaitaiNettyTcpExample;

import java.util.List;

public class SimpleKaitaiDecoder extends ByteToMessageDecoder { // (1)

    private static final Logger logger = LogManager.getLogger(SimpleKaitaiDecoder.class);
    private ByteBuf buf;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) { // (2)
        KaitaiNettyTcpExample exampleDecoder = new KaitaiNettyTcpExample(new ByteBufferKaitaiStream(in.nioBuffer()));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Exception :", cause);
        //cause.printStackTrace();
        ctx.close();
    }


}

