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
		_pmem_start = . ;
		_sdata = . ;
    *(.data)
		_edata = . ;
  }>SRAM : data
	/* store the end address of the data section*/
  .bss : 
	{
		_bss_start = . ;
    *(.bss*)
    *(.sbss*)
    *(.scommon)
  }>SRAM
	.stack (NOLOAD):
	{
		. = ALIGN(0x1000);
		_stack_top = . ;
		. = _stack_top + 0x1000 ; 
		_stack_pointer = . ;
		end = . ;
		_end = . ;
	} > SRAM
	. = ALIGN(0x1000);
	_heap_start = .;
	end = .;
	_end = .;
}
