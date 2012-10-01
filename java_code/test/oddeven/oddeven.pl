program oddeven

  var integer i

  function boolean even (integer i)
    
    procedure ev (integer i)

      procedure od (integer i)

        begin // od[d]
          if i = 0 then
            even := false
          else
            call ev(i-1)
          endif
        end

      begin // ev[en]
        if i = 0 then
          even := true
        else
          call od(i-1)
        endif
      end

    begin // even
      call ev(i)
    end
  
  function boolean odd (integer i)
    begin // odd
      odd := not even(i)
    end

  begin
    i := 0
    while i <= 10 do
      write i
      writec ' '
      if odd(i) then writec "odd"
      else writec "even"
      endif
      writec '\n'
      i := i+1
    endwhile
  end 
