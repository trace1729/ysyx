ENTRY(_start)

MEMORY {
	MROM (rx): ORIGIN = 0x20000000, LENGTH = 0x1000
	SRAM (rwx): ORIGIN = 0x0f000000, LENGTH = 0x2000
}

PHDRS { text PT_LOAD; data PT_LOAD; }

SECTIONS {
  .text : 
	{
    *(entry)
    *(.text*)
		_etext = . ;
  }>MROM : text
	/* store the end address of the text section*/
  .rodata : 
	{
    *(.rodata*)
  }>MROM
  .data : 
	{
		_sdata = . ;
    *(.data)
		_edata = . ;
  }>MROM : data
	/* store the end address of the data section*/
  .bss : 
	{
		_bss_start = . ;
    *(.bss*)
    *(.sbss*)
    *(.scommon)
  }>MROM : data
	.stack (NOLOAD):
	{
		. = ORIGIN(SRAM) + LENGTH(SRAM) - 0x1000 ;
		_stack_top = . ;
		_stack_pointer = .  + 0x1000;
	}
	_heap_start = ORIGIN(SRAM);
	_heap_end = _stack_top;
	end = . ;
	_end = . ;
}
