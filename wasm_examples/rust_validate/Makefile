.PHONY: build

build:
	cargo build --target wasm32-wasip1
	cp ./target/wasm32-wasip1/debug/module.wasm ../rust_validate.wasm
	wasm2wat ../rust_validate.wasm > ../rust_validate.wat