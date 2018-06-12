package com.bonree.brfs.common.http.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

import com.bonree.brfs.common.http.HttpConfig;
import com.bonree.brfs.common.utils.LifeCycle;

/**
 * Netty实现的Http Server启动类
 * 
 * @author chen
 *
 */
public class NettyHttpServer implements LifeCycle {
	private ChannelFuture channelFuture;
	
	private NettyChannelInitializer handlerInitializer;
	
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	
	private HttpConfig httpConfig;
	
	public NettyHttpServer(HttpConfig httpConfig) {
		this.httpConfig = httpConfig;
		this.handlerInitializer = new NettyChannelInitializer();
		this.bossGroup = new NioEventLoopGroup(httpConfig.getAcceptWorkerNum());
		this.workerGroup = new NioEventLoopGroup(httpConfig.getRequestHandleWorkerNum());
	}
	
	@Override
	public void start() throws InterruptedException {
		ServerBootstrap serverStart = new ServerBootstrap();
		serverStart.group(bossGroup, workerGroup);
		serverStart.channel(NioServerSocketChannel.class);
		serverStart.childHandler(handlerInitializer);
		serverStart.option(ChannelOption.SO_BACKLOG, httpConfig.getBacklog());//积压数量
		serverStart.option(ChannelOption.CONNECT_TIMEOUT_MILLIS,httpConfig.getConnectTimeoutMillies());//连接超时时间(毫秒)
		serverStart.childOption(ChannelOption.SO_KEEPALIVE, httpConfig.isKeepAlive());//保持连接
		serverStart.childOption(ChannelOption.TCP_NODELAY, httpConfig.isTcpNoDelay());
		serverStart.childOption(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator());
		serverStart.childOption(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT);
		
		InetSocketAddress address = (httpConfig.getHost() == null ?
				new InetSocketAddress(httpConfig.getPort()) : new InetSocketAddress(httpConfig.getHost(), httpConfig.getPort()));
		channelFuture = serverStart.bind(address).sync();
	}

	@Override
	public void stop() {
		try {
			channelFuture.channel().close().sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}
	
	public void addHttpAuthenticator(HttpAuthenticator authenticator) {
		handlerInitializer.addAuthenticationHandler(new NettyHttpAuthenticationHandler(authenticator));
	}
	
	public void addContextHandler(NettyHttpContextHandler contextHttpHandler) {
		handlerInitializer.addContextHandler(contextHttpHandler);
	}
}
